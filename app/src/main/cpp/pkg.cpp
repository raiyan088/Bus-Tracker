#include "pkg.h"

static uint32_t m_pos = 0;
static size_t m_length = 0;
static struct element *head = NULL;
static struct element *tail = NULL;

static uint32_t pkcs7HelperLenNum(unsigned char lenbyte) {
    uint32_t num = 1;
    if (lenbyte & 0x80) {
        num += lenbyte & 0x7f;
    }
    return num;
}

static uint32_t pkcs7HelperGetLength(unsigned char *certrsa, unsigned char lenbyte, int offset) {
    int32_t len = 0, num;
    unsigned char tmp;
    if (lenbyte & 0x80) {
        num = lenbyte & 0x7f;
        if (num < 0 || num > 4) {
            return 0;
        }
        while (num) {
            len <<= 8;
            tmp = certrsa[offset++];
            len += (tmp & 0xff);
            num--;
        }
    } else {
        len = lenbyte & 0xff;
    }
    assert(len >= 0);
    return (uint32_t) len;
}

int32_t pkcs7HelperCreateElement(unsigned char *certrsa, unsigned char tag, char *name, int level) {
    unsigned char get_tag = certrsa[m_pos++];
    if (get_tag != tag) {
        m_pos--;
        return -1;
    }
    unsigned char lenbyte = certrsa[m_pos];
    int len = pkcs7HelperGetLength(certrsa, lenbyte, m_pos + 1);
    m_pos += pkcs7HelperLenNum(lenbyte);

    element *node = (element *) calloc(1, sizeof(element));
    node->tag = get_tag;
    strcpy(node->name, name);
    node->begin = m_pos;
    node->len = len;
    node->level = level;
    node->next = NULL;

    if (head == NULL) {
        head = tail = node;
    } else {
        tail->next = node;
        tail = node;
    }
    return len;
}

bool pkcs7HelperParseCertificate(unsigned char *certrsa, int level) {
    char *names[] = {
            "tbsCertificate",
            "version",
            "serialNumber",
            "signature",
            "issuer",
            "validity",
            "subject",
            "subjectPublicKeyInfo",
            "issuerUniqueID-[optional]",
            "subjectUniqueID-[optional]",
            "extensions-[optional]",
            "signatureAlgorithm",
            "signatureValue"};
    int len = 0;
    unsigned char tag;

    len = pkcs7HelperCreateElement(certrsa, TAG_SEQUENCE, names[0], level);
    if (len == -1 || m_pos + len > m_length) {
        return false;
    }
    //version
    tag = certrsa[m_pos];
    if (((tag & 0xc0) == 0x80) && ((tag & 0x1f) == 0)) {
        m_pos += 1;
        m_pos += pkcs7HelperLenNum(certrsa[m_pos]);
        len = pkcs7HelperCreateElement(certrsa, TAG_INTEGER, names[1], level + 1);
        if (len == -1 || m_pos + len > m_length) {
            return false;
        }
        m_pos += len;
    }

    for (int i = 2; i < 11; i++) {
        switch (i) {
            case 2:
                tag = TAG_INTEGER;
                break;
            case 8:
                tag = 0xA1;
                break;
            case 9:
                tag = 0xA2;
                break;
            case 10:
                tag = 0xA3;
                break;
            default:
                tag = TAG_SEQUENCE;
        }
        len = pkcs7HelperCreateElement(certrsa, tag, names[i], level + 1);
        if (i < 8 && len == -1) {
            return false;
        }
        if (len != -1)
            m_pos += len;
    }
    //signatureAlgorithm
    len = pkcs7HelperCreateElement(certrsa, TAG_SEQUENCE, names[11], level);
    if (len == -1 || m_pos + len > m_length) {
        return false;
    }
    m_pos += len;
    //signatureValue
    len = pkcs7HelperCreateElement(certrsa, TAG_BITSTRING, names[12], level);
    if (len == -1 || m_pos + len > m_length) {
        return false;
    }
    m_pos += len;
    return true;
}

/**
 * Resolve signer information
 */
bool pkcs7HelperParseSignerInfo(unsigned char *certrsa, int level) {
    char *names[] = {
            "version",
            "issuerAndSerialNumber",
            "digestAlgorithmId",
            "authenticatedAttributes-[optional]",
            "digestEncryptionAlgorithmId",
            "encryptedDigest",
            "unauthenticatedAttributes-[optional]"};
    int len;
    unsigned char tag;
    for (int i = 0; i < sizeof(names) / sizeof(names[0]); i++) {
        switch (i) {
            case 0:
                tag = TAG_INTEGER;
                break;
            case 3:
                tag = 0xA0;
                break;
            case 5:
                tag = TAG_OCTETSTRING;
                break;
            case 6:
                tag = 0xA1;
                break;
            default:
                tag = TAG_SEQUENCE;

        }
        len = pkcs7HelperCreateElement(certrsa, tag, names[i], level);
        if (len == -1 || m_pos + len > m_length) {
            if (i == 3 || i == 6)
                continue;
            return false;
        }
        m_pos += len;
    }
    return m_pos == m_length;
}

bool pkcs7HelperParseContent(unsigned char *certrsa, int level) {

    char *names[] = {"version",
                     "DigestAlgorithms",
                     "contentInfo",
                     "certificates-[optional]",
                     "crls-[optional]",
                     "signerInfos",
                     "signerInfo"};

    unsigned char tag;
    int len = 0;
    //version
    len = pkcs7HelperCreateElement(certrsa, TAG_INTEGER, names[0], level);
    if (len == -1 || m_pos + len > m_length) {
        return false;
    }
    m_pos += len;
    //DigestAlgorithms
    len = pkcs7HelperCreateElement(certrsa, TAG_SET, names[1], level);
    if (len == -1 || m_pos + len > m_length) {
        return false;
    }
    m_pos += len;
    //contentInfo
    len = pkcs7HelperCreateElement(certrsa, TAG_SEQUENCE, names[2], level);
    if (len == -1 || m_pos + len > m_length) {
        return false;
    }
    m_pos += len;
    //certificates-[optional]
    tag = certrsa[m_pos];
    if (tag == TAG_OPTIONAL) {
        m_pos++;
        m_pos += pkcs7HelperLenNum(certrsa[m_pos]);
        len = pkcs7HelperCreateElement(certrsa, TAG_SEQUENCE, names[3], level);
        if (len == -1 || m_pos + len > m_length) {
            return false;
        }
        bool ret = pkcs7HelperParseCertificate(certrsa, level + 1);
        if (ret == false) {
            return ret;
        }
    }
    //crls-[optional]
    tag = certrsa[m_pos];
    if (tag == 0xA1) {
        m_pos++;
        m_pos += pkcs7HelperLenNum(certrsa[m_pos]);
        len = pkcs7HelperCreateElement(certrsa, TAG_SEQUENCE, names[4], level);
        if (len == -1 || m_pos + len > m_length) {
            return false;
        }
        m_pos += len;
    }
    //signerInfos
    tag = certrsa[m_pos];
    if (tag != TAG_SET) {
        return false;
    }
    len = pkcs7HelperCreateElement(certrsa, TAG_SET, names[5], level);
    if (len == -1 || m_pos + len > m_length) {
        return false;
    }
    //signerInfo
    len = pkcs7HelperCreateElement(certrsa, TAG_SEQUENCE, names[6], level + 1);
    if (len == -1 || m_pos + len > m_length) {
        return false;
    }
    return pkcs7HelperParseSignerInfo(certrsa, level + 2);
}

/**
 *Finds the element in pkcs7 by name, returns NULL if it is not found.
 *name:
 *begin: beginning of the search
 */
static element *pkcs7HelperGetElement(const char *name, element *begin) {
    if (begin == NULL)
        begin = head;
    element *p = begin;
    while (p != NULL) {
        if (strncmp(p->name, name, strlen(name)) == 0) {
            return p;
        }

        p = p->next;
    }
    return p;
}

static bool pkcs7HelperParse(unsigned char *certrsa, size_t length) {
    unsigned char tag, lenbyte;
    int len = 0;
    int level = 0;
    m_pos = 0;
    m_length = length;

    tag = certrsa[m_pos++];
    if (tag != TAG_SEQUENCE) {
        return false;
    }
    lenbyte = certrsa[m_pos];
    len = pkcs7HelperGetLength(certrsa, lenbyte, m_pos + 1);
    m_pos += pkcs7HelperLenNum(lenbyte);
    if (m_pos + len > m_length)
        return false;
    //contentType
    len = pkcs7HelperCreateElement(certrsa, TAG_OBJECTID, "contentType", level);
    if (len == -1) {
        return false;
    }
    m_pos += len;
    //optional
    tag = certrsa[m_pos++];
    lenbyte = certrsa[m_pos];
    m_pos += pkcs7HelperLenNum(lenbyte);
    //content-[optional]
    len = pkcs7HelperCreateElement(certrsa, TAG_SEQUENCE, "content-[optional]", level);
    if (len == -1) {
        return false;
    }
    return pkcs7HelperParseContent(certrsa, level + 1);
}

#ifndef NDEBUG

static void pkcs7HelperPrint() {
    element *p = head;
    const size_t PRINT_BUF_SIZE = 256;
    char buf[PRINT_BUF_SIZE] = "";
    while (p != NULL) {
        for (int i = 0; i < p->level; i++) {
            sprintf(buf, "%s    ", buf);
        }
        sprintf(buf, "%s %s", buf, p->name);

        for (int i = 0; i < 40 - strlen(p->name) - 4 * p->level; i++) {
            sprintf(buf, "%s ", buf);
        }
        sprintf(buf, "%s%6d(0x%02x)", buf, p->begin, p->begin);
        int num = 0;
        int size = p->begin;
        while (size) {
            num += 1;
            size >>= 4;
        }
        if (num < 2) num = 2;
        for (int i = 0; i < 8 - num; i++) {
            sprintf(buf, "%s ", buf);
        }
        sprintf(buf, "%s%4d(0x%02x)", buf, (int) p->len, (unsigned int) p->len);
        memset(buf, 0, PRINT_BUF_SIZE);
        p = p->next;
    }
}


#endif //NDEBUG

/**
 * Convert length information to ASN.1 length format
 * len <= 0x7f       1
 * len >= 0x80       1 + Non-zero bytes
 */
static size_t pkcs7HelperGetNumFromLen(size_t len) {
    size_t num = 0;
    size_t tmp = len;
    while (tmp) {
        num++;
        tmp >>= 8;
    }
    if ((num == 1 && len >= 0x80) || (num > 1))
        num += 1;
    return num;
}


/**
 *Each element element is a {tag, length, data} triple, tag and length are saved by tag and len, and data is saved by [begin, begin+len].
 *
 *This function calculates the offset from the data position to the tag position
 */
size_t pkcs7HelperGetTagOffset(element *p, unsigned char *certrsa) {
    if (p == NULL)
        return 0;
    size_t offset = pkcs7HelperGetNumFromLen(p->len);
    if (certrsa[p->begin - offset - 1] == p->tag)
        return offset + 1;
    else
        return 0;
}

unsigned char *pkgGetSignature(unsigned char *certrsa, size_t len_in, size_t *len_out) {
    if (pkcs7HelperParse(certrsa, len_in)) {

        element *p_cert = pkcs7HelperGetElement("certificates-[optional]", head);
        if (!p_cert) {
            return NULL;
        }
        size_t offset = pkcs7HelperGetTagOffset(p_cert, certrsa);
        if (offset == 0) {
            printf("get offset error!\n");
            return NULL;
        }
        *len_out = p_cert->len + offset;
        return certrsa + p_cert->begin - offset;
    }

    return NULL;
}

void pkgFree() {
    element *p = head;
    while (p != NULL) {
        head = p->next;
        free(p);
        p = head;
    }
    head = NULL;
}