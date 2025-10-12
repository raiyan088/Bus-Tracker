#ifndef BUBT_BUS_TRACKER_PKG_H
#define BUBT_BUS_TRACKER_PKG_H

#include <stdbool.h>
#include <assert.h>
#include <malloc.h>
#include <string.h>
#include <unistd.h>

#define TAG_INTEGER         0x02
#define TAG_BITSTRING       0x03
#define TAG_OCTETSTRING     0x04
#define TAG_NULL            0x05
#define TAG_OBJECTID        0x06
#define TAG_UTCTIME         0x17
#define TAG_GENERALIZEDTIME 0x18
#define TAG_SEQUENCE        0x30
#define TAG_SET             0x31

#define TAG_OPTIONAL    0xA0


#define NAME_LEN    63

typedef struct element {
    unsigned char tag;
    char name[NAME_LEN];
    int begin;
    size_t len;
    int level;
    struct element *next;
} element;

unsigned char * pkgGetSignature(unsigned char * certrsa, size_t len_in, size_t *len_out);

void pkgFree();

#endif