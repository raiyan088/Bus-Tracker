#ifndef BUBT_BUS_TRACKER_MD5_H
#define BUBT_BUS_TRACKER_MD5_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

void md5(const uint8_t *initial_msg, size_t initial_len, uint8_t *digest);

#ifdef __cplusplus
}
#endif

#endif
