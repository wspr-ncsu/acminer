/* xwrap.c - wrappers around existing library functions.
 *
 * Functions with the x prefix are wrappers that either succeed or kill the
 * program with an error message, but never return failure. They usually have
 * the same arguments and return value as the function they wrap.
 *
 */

#define _GNU_SOURCE
#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <dirent.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mount.h>
#include <sys/mman.h>
//#include <sys/sendfile.h>

#include "magisk.h"
#include "utils.h"

FILE *xfopen(const char *pathname, const char *mode) {
	FILE *fp = fopen(pathname, mode);
	if (fp == NULL) {
		PLOGE("fopen: %s", pathname);
	}
	return fp;
}

FILE *xfdopen(int fd, const char *mode) {
	FILE *fp = fdopen(fd, mode);
	if (fp == NULL) {
		PLOGE("fopen");
	}
	return fp;
}

int xopen2(const char *pathname, int flags) {
	int fd = open(pathname, flags);
	if (fd < 0) {
		PLOGE("open: %s", pathname);
	}
	return fd;
}

int xopen3(const char *pathname, int flags, mode_t mode) {
	int fd = open(pathname, flags, mode);
	if (fd < 0) {
		PLOGE("open: %s", pathname);
	}
	return fd;
}

ssize_t xwrite(int fd, const void *buf, size_t count) {
	int ret = write(fd, buf, count);
	if (count != ret) {
		PLOGE("write");
	}
	return ret;
}

// Read error other than EOF
ssize_t xread(int fd, void *buf, size_t count) {
	int ret = read(fd, buf, count);
	if (ret < 0) {
		PLOGE("read");
	}
	return ret;
}

// Read exact same size as count
ssize_t xxread(int fd, void *buf, size_t count) {
	int ret = read(fd, buf, count);
	if (count != ret) {
		PLOGE("read");
	}
	return ret;
}

int xpipe2(int pipefd[2], int flags) {
	int ret = pipe2(pipefd, flags);
	if (ret == -1) {
		PLOGE("pipe2");
	}
	return ret;
}
/*
int xsetns(int fd, int nstype) {
	int ret = setns(fd, nstype);
	if (ret == -1) {
		PLOGE("setns");
	}
	return ret;
}
*/
DIR *xopendir(const char *name) {
	DIR *d = opendir(name);
	if (d == NULL) {
		PLOGE("opendir: %s", name);
	}
	return d;
}

struct dirent *xreaddir(DIR *dirp) {
	errno = 0;
	struct dirent *e = readdir(dirp);
	if (errno && e == NULL) {
		PLOGE("readdir");
	}
	return e;
}

pid_t xsetsid() {
	pid_t pid = setsid();
	if (pid == -1) {
		PLOGE("setsid");
	}
	return pid;
}

int xsocket(int domain, int type, int protocol) {
	int fd = socket(domain, type, protocol);
	if (fd == -1) {
		PLOGE("socket");
	}
	return fd;
}

int xbind(int sockfd, const struct sockaddr *addr, socklen_t addrlen) {
	int ret = bind(sockfd, addr, addrlen);
	if (ret == -1) {
		PLOGE("bind");
	}
	return ret;
}

int xconnect(int sockfd, const struct sockaddr *addr, socklen_t addrlen) {
	int ret = connect(sockfd, addr, addrlen);
	if (ret == -1) {
		PLOGE("bind");
	}
	return ret;
}

int xlisten(int sockfd, int backlog) {
	int ret = listen(sockfd, backlog);
	if (ret == -1) {
		PLOGE("listen");
	}
	return ret;
}

int xaccept4(int sockfd, struct sockaddr *addr, socklen_t *addrlen, int flags) {
	int fd = accept4(sockfd, addr, addrlen, flags);
	if (fd == -1) {
		PLOGE("accept");
	}
	return fd;
}

void *xmalloc(size_t size) {
	void *p = malloc(size);
	if (p == NULL) {
		PLOGE("malloc");
	}
	return p;
}

void *xcalloc(size_t nmemb, size_t size) {
	void *p = calloc(nmemb, size);
	if (p == NULL) {
		PLOGE("calloc");
	}
	return p;
}

void *xrealloc(void *ptr, size_t size) {
	void *p = realloc(ptr, size);
	if (p == NULL) {
		PLOGE("realloc");
	}
	return p;
}

ssize_t xsendmsg(int sockfd, const struct msghdr *msg, int flags) {
	int sent = sendmsg(sockfd, msg, flags);
	if (sent == -1) {
		PLOGE("sendmsg");
	}
	return sent;
}

ssize_t xrecvmsg(int sockfd, struct msghdr *msg, int flags) {
	int rec = recvmsg(sockfd, msg, flags);
	if (rec == -1) {
		PLOGE("recvmsg");
	}
	return rec;
}

int xpthread_create(pthread_t *thread, const pthread_attr_t *attr,
                          void *(*start_routine) (void *), void *arg) {
	errno = pthread_create(thread, attr, start_routine, arg);
	if (errno) {
		PLOGE("pthread_create");
	}
	return errno;
}

int xsocketpair(int domain, int type, int protocol, int sv[2]) {
	int ret = socketpair(domain, type, protocol, sv);
	if (ret == -1) {
		PLOGE("socketpair");
	}
	return ret;
}

int xstat(const char *pathname, struct stat *buf) {
	int ret = stat(pathname, buf);
	if (ret == -1) {
		PLOGE("stat %s", pathname);
	}
	return ret;
}

int xlstat(const char *pathname, struct stat *buf) {
	int ret = lstat(pathname, buf);
	if (ret == -1) {
		PLOGE("lstat %s", pathname);
	}
	return ret;
}

int xdup2(int oldfd, int newfd) {
	int ret = dup2(oldfd, newfd);
	if (ret == -1) {
		PLOGE("dup2");
	}
	return ret;
}

ssize_t xreadlink(const char *pathname, char *buf, size_t bufsiz) {
	ssize_t ret = readlink(pathname, buf, bufsiz);
	if (ret == -1) {
		PLOGE("readlink %s", pathname);
	} else {
		buf[ret] = '\0';
		++ret;
	}
	return ret;
}

int xsymlink(const char *target, const char *linkpath) {
	int ret = symlink(target, linkpath);
	if (ret == -1) {
		PLOGE("symlink %s->%s", target, linkpath);
	}
	return ret;
}

int xmount(const char *source, const char *target,
	const char *filesystemtype, unsigned long mountflags,
	const void *data) {
	int ret = mount(source, target, filesystemtype, mountflags, data);
	if (ret == -1) {
		PLOGE("mount %s->%s", source, target);
	}
	return ret;
}

int xumount(const char *target) {
	int ret = umount(target);
	if (ret == -1) {
		PLOGE("umount %s", target);
	}
	return ret;
}

int xumount2(const char *target, int flags) {
	int ret = umount2(target, flags);
	if (ret == -1) {
		PLOGE("umount2 %s", target);
	}
	return ret;
}

int xchmod(const char *pathname, mode_t mode) {
	int ret = chmod(pathname, mode);
	if (ret == -1) {
		PLOGE("chmod %s %u", pathname, mode);
	}
	return ret;
}

int xrename(const char *oldpath, const char *newpath) {
	int ret = rename(oldpath, newpath);
	if (ret == -1) {
		PLOGE("rename %s->%s", oldpath, newpath);
	}
	return ret;
}

int xmkdir(const char *pathname, mode_t mode) {
	int ret = mkdir(pathname, mode);
	if (ret == -1 && errno != EEXIST) {
		PLOGE("mkdir %s %u", pathname, mode);
	}
	return ret;
}

void *xmmap(void *addr, size_t length, int prot, int flags,
	int fd, off_t offset) {
	void *ret = mmap(addr, length, prot, flags, fd, offset);
	if (ret == MAP_FAILED) {
		PLOGE("mmap");
	}
	return ret;
}

ssize_t xsendfile(int out_fd, int in_fd, off_t *offset, size_t count) {
	PLOGE("No sendfile");
	return 0;
/*
	ssize_t ret = sendfile(out_fd, in_fd, offset, count);
	if (count != ret) {
		PLOGE("sendfile");
	}
	return ret;
*/
}

int xmkdir_p(const char *pathname, mode_t mode) {
	int ret = mkdir_p(pathname, mode);
	if (ret == -1) {
		PLOGE("mkdir_p %s", pathname);
	}
	return ret;
}
