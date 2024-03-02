#
# TOP Makefile
#
DIR_DADAO_TOP		:= $(shell pwd)
DIR_DADAO_SOURCE	:= $(DIR_DADAO_TOP)/__source
DIR_DADAO_BUILD		:= $(DIR_DADAO_TOP)/__build
DIR_DADAO_INSTALL	:= $(DIR_DADAO_TOP)/__install
DIR_DADAO_TARGET	:= $(DIR_DADAO_TOP)/__dadao
DIR_DADAO_LOG		:= $(DIR_DADAO_TOP)/__log

VER_BINUTILS_GDB	:= 0235
VER_GCC			:= 1003
VER_GLIBC		:= 0231
VER_NEWLIB_CYGWIN	:= 0303
VER_LLVM		:= 1600
VER_LINUX		:= 0504
VER_QEMU		:= 0801
VER_CHIPYARD		:= 0107

_DADAO_CORES_		:= $(shell expr `nproc` / 1)
DADAO_PATH		:= $(DIR_DADAO_INSTALL)/bin/:$(DIR_DADAO_INSTALL)/usr/bin:/bin:/usr/bin
DADAO_MAKE		:= make --silent DIR_DADAO_TOP=$(DIR_DADAO_TOP) PATH=$(DADAO_PATH) --jobs=$(_DADAO_CORES_)

all:
	@echo ""
	@echo "DA DAO ZHI JIAN!"
	@echo "- VERSION 0.4 -"
	@echo ""

include TCH-binutils-gdb/Makefrag
include TCH-gcc/Makefrag
include TCH-glibc/Makefrag
include TCH-newlib-cygwin/Makefrag
include TCH-llvm/Makefrag
include ENV-linux/Makefrag
include SIM-qemu/Makefrag
include SOC-chipyard/Makefrag

include DADAO-opcodes/Makefrag
include DADAO-rte/Makefrag
include DADAO-tests/Makefrag
include ENV-proxylinux/Makefrag

tch-gnu-highfive:
	@echo "=== BUILD Toolchain dadao-linux-gnu BEGIN ==="
	@make -s BINUTILS_GDB_$(VER_BINUTILS_GDB)_TARGET=dadao-linux-gnu binutils-gdb-$(VER_BINUTILS_GDB)-highfive
	@make -s GCC_$(VER_GCC)_TARGET=dadao-linux-gnu gcc-$(VER_GCC)-highfive
	@make -s glibc-$(VER_GLIBC)-highfive
	@echo "=== BUILD Toolchain dadao-linux-gnu DONE! ==="

tch-elf-highfive:
	@echo "=== BUILD Toolchain dadao-unknown-elf BEGIN ==="
	@echo "=== building time maybe: real 11m, user 34m, sys 5m ==="
	@make -s binutils-gdb-$(VER_BINUTILS_GDB)-highfive
	@make -s gcc-$(VER_GCC)-highfive
	@make -s newlib-cygwin-$(VER_NEWLIB_CYGWIN)-highfive
	@echo "=== BUILD Toolchain dadao-unknown-elf DONE! ==="

env-highfive:
	@echo "=== BUILD Run-Time Environment BEGIN ==="
	@make -s linux-$(VER_LINUX)-highfive
	@echo "=== BUILD Run-Time Environment DONE! ==="

sim-highfive:
	@echo "=== BUILD Simulators BEGIN ==="
	@make -s qemu-$(VER_QEMU)-highfive
	@echo "=== BUILD Simulators DONE! ==="

soc-highfive:
	@echo "=== BUILD SoC BEGIN ==="
	@make -s chipyard-$(VER_CHIPYARD)-highfive
	@echo "=== BUILD SoC DONE! ==="

dadao-highfive:
	@echo "=== BUILD BEGIN ==="
	@make --silent tch-elf-highfive
	@make --silent sim-highfive
	@make --silent soc-highfive
	@echo "=== BUILD DONE! ==="
	@echo "=== TEST@qemu  BEGIN ==="
	@make --silent tests-isa-qemu-highfive
	@make --silent tests-bmarks-qemu-dhrystone-highfive
#	@make --silent tests-bmarks-qemu-embench-highfive
	@echo "=== TEST@qemu DONE! ==="
	@echo "=== TEST@bare BEGIN ==="
	@make --silent tests-isa-bare-highfive
	@make --silent tests-bmarks-bare-highfive
	@echo "=== TEST@bare DONE! ==="

dadao-clean:
	@echo "CLEAR EVERYTHING!"
	@rm -fr __*
	@echo "CLEAR EVERYTHING DONE!"
