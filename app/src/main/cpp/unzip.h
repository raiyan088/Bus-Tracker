#ifndef BUBT_BUS_TRACKER_UNZIP_H
#define BUBT_BUS_TRACKER_UNZIP_H

#include <zlib.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include <time.h>
#include <unistd.h>

#include "third/minizip/mz.h"
#include "third/minizip/mz_os.h"
#include "third/minizip/mz_strm.h"
#include "third/minizip/mz_strm_mem.h"
#include "third/minizip/mz_strm_bzip.h"
#include "third/minizip/mz_strm_zlib.h"
#include "third/minizip/mz_zip.h"
#include "third/minizip/mz_strm_split.h"
#include "third/minizip/mz_strm_buf.h"


unsigned char * unzipGetCertificateDetails(const char * fullApkPath, size_t * len);

#endif
