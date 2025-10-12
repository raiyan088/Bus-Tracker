#include "unzip.h"

int string_starts_with(const char *str, const char *prefix){
    size_t str_len = strlen(str);
    size_t prefix_len = strlen(prefix);
    return str_len < prefix_len ? 0 : strncasecmp(prefix, str, prefix_len) == 0;
}


//return 1 if str ands with suffix
int string_ends_with(const char *str, const char *suffix){
    size_t str_len = strlen(str);
    size_t suffix_len = strlen(suffix);
    return str_len < suffix_len ? 0 : strcasecmp(str + (str_len-suffix_len), suffix) == 0;
}


//return MZ_ERROR
static int32_t unzipHelperGetCertFileInfo(void *handle, mz_zip_file **file_info) {

    int32_t err = MZ_OK;

    err = mz_zip_goto_first_entry(handle);

    if (err != MZ_OK && err != MZ_END_OF_LIST) {
        return err;
    }

    while (err == MZ_OK) {
        err = mz_zip_entry_get_info(handle, file_info);

        if (err != MZ_OK) {
            *file_info = NULL;
            break;
        }

        //Return MZ_OK if is a certificate file
        if (NULL != (*file_info)->filename && string_starts_with((*file_info)->filename, "META-INF/")) {
            if(string_ends_with((*file_info)->filename, ".RSA")
               || string_ends_with((*file_info)->filename, ".DSA")
               || string_ends_with((*file_info)->filename, ".EC")){
                return MZ_OK;
            }
        }

        err = mz_zip_goto_next_entry(handle);

        if (err != MZ_OK && err != MZ_END_OF_LIST) {
            *file_info = NULL;
            return err;
        }
    }

    *file_info = NULL;

    if (err == MZ_END_OF_LIST) {
        return MZ_OK;
    }
    return err;
}

static void unzipHelperPrintFileInfo(const mz_zip_file *file_info) {
    uint32_t ratio = 0;
    struct tm tmu_date;
    const char *string_method = NULL;
    char crypt = ' ';

    ratio = 0;
    if (file_info->uncompressed_size > 0)
        ratio = (uint32_t)((file_info->compressed_size * 100) / file_info->uncompressed_size);

    // Display a '*' if the file is encrypted
    if (file_info->flag & MZ_ZIP_FLAG_ENCRYPTED)
        crypt = '*';

    switch (file_info->compression_method)
    {
        case MZ_COMPRESS_METHOD_RAW:
            string_method = "Stored";
            break;
        case MZ_COMPRESS_METHOD_DEFLATE:
            string_method = "Deflate";
            break;
        case MZ_COMPRESS_METHOD_BZIP2:
            string_method = "BZip2";
            break;
        case MZ_COMPRESS_METHOD_LZMA:
            string_method = "LZMA";
            break;
        default:
            string_method = "Unknown";
    }

    mz_zip_time_t_to_tm(file_info->modified_date, &tmu_date);
}

unsigned char *unzipGetCertificateDetails(const char *fullApkPath, size_t *len) {

    unsigned char *result = NULL;
    int32_t err = 0;
    int32_t read_file = 0;

    void *handle = NULL;
    void *file_stream = NULL;
    void *split_stream = NULL;
    void *buf_stream = NULL;
    char *password = NULL;

    int64_t disk_size = 0;
    int16_t mode = MZ_OPEN_MODE_READ;
    int32_t err_close = 0;

    mz_stream_os_create(&file_stream);
    mz_stream_buffered_create(&buf_stream);
    mz_stream_split_create(&split_stream);

    mz_stream_set_base(split_stream, file_stream);

    mz_stream_split_set_prop_int64(split_stream, MZ_STREAM_PROP_DISK_SIZE, disk_size);

    err = mz_stream_open(split_stream, fullApkPath, mode);
    mz_zip_file *file_info = NULL;
    if (err == MZ_OK) {
        handle = mz_zip_open(split_stream, mode);

        if (handle == NULL) {
            err = MZ_FORMAT_ERROR;
        } else {
            err = unzipHelperGetCertFileInfo(handle, &file_info);
            if (err == MZ_OK && NULL != file_info) {
                unzipHelperPrintFileInfo(file_info);
                //unzip
                err = mz_zip_entry_read_open(handle, 0, password);
                if (err == MZ_OK) {
                    result = static_cast<unsigned char *>(calloc(file_info->uncompressed_size,
                                                                 sizeof(unsigned char)));
                    if (NULL != result) {
                        read_file = mz_zip_entry_read(handle, result,
                                                      (uint32_t) (file_info->uncompressed_size));
                        if (read_file < 0) {
                            free(result);
                            result = NULL;
                            err = read_file;
                        } else {
                            *len = (size_t) read_file;
                        }
                    }
                }
            }
        }

        err_close = mz_zip_close(handle);

        if (err_close != MZ_OK) {
            err = err_close;
        }

        mz_stream_close(split_stream);

    }
    mz_stream_split_delete(&split_stream);
    mz_stream_buffered_delete(&buf_stream);
    mz_stream_os_delete(&file_stream);

    return result;
}