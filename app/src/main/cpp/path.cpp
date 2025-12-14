#include "path.h"

static char *getPackageName() {
    const size_t BUFFER_SIZE = 256;
    char buffer[BUFFER_SIZE] = "";
    int fd = open("/proc/self/cmdline", O_RDONLY);
    if (fd > 0) {
        ssize_t r = read(fd, buffer, BUFFER_SIZE - 1);
        close(fd);
        if (r > 0) {
            return strdup(buffer);
        }
    }
    return nullptr;
}

static const char *getFilenameExt(const char *filename) {
    const char *dot = strrchr(filename, '.');
    if (!dot || dot == filename) return "";
    return dot + 1;
}

char *pathHelperGetPath() {

    char *package = getPackageName();
    if (nullptr == package) {
        return nullptr;
    }

    FILE *fp = fopen("/proc/self/maps", "r");
    if (nullptr == fp) {
        free(package);
        return nullptr;
    }
    const size_t BUFFER_SIZE = 256;
    char buffer[BUFFER_SIZE] = "";
    char path[BUFFER_SIZE] = "";

    bool find = false;
    while (fgets(buffer, BUFFER_SIZE, fp)) {
        if (sscanf(buffer, "%*llx-%*llx %*s %*s %*s %*s %s", path) == 1) {
            if (strstr(path, package)) {
                char *bname = basename(path);
                if (strcasecmp(getFilenameExt(bname), "apk") == 0) {
                    find = true;
                    break;
                }
            }
        }
    }
    fclose(fp);
    free(package);
    if (find) {
        return strdup(path);
    }
    return nullptr;
}