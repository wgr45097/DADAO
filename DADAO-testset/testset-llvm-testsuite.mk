#
# Makefile for llvm-testsuite
#

LLVM_TESTSUITE_GITHUB		:= https://github.com/llvm/llvm-test-suite.git
LLVM_TESTSUITE_LOCAL		:= /pub/GITHUB/llvm/llvm-test-suite.git
LLVM_TESTSUITE_VERSION		:= llvmorg-16.0.0
LLVM_TESTSUITE_BRANCH		:= dadao-1600

LLVM_TESTSUITE_NEWFILES		:= $(DIR_DADAO_TOP)/DADAO-testset/llvm-testsuite-newfiles
LLVM_TESTSUITE_PATCHES		:= $(DIR_DADAO_TOP)/DADAO-testset/llvm-testsuite-patches

LLVM_TESTSUITE_SOURCE		:= $(DIR_DADAO_SOURCE)/llvm-testsuite
LLVM_TESTSUITE_BUILD		:= $(DIR_DADAO_BUILD)/llvm-testsuite
LLVM_TESTSUITE_LOG_STDOUT	:= $(DIR_DADAO_LOG)/llvm-testsuite.out
LLVM_TESTSUITE_LOG_STDERR	:= $(DIR_DADAO_LOG)/llvm-testsuite.err

testset-llvm-testsuite-clean:
	# Remove old llvm-testsuite source dir ...
	@rm -fr $(LLVM_TESTSUITE_SOURCE)
	# Remove old llvm-testsuite build dir ...
	@rm -fr $(LLVM_TESTSUITE_BUILD)

testset-llvm-testsuite-source:
	@test -d $(DIR_DADAO_SOURCE) || mkdir -p $(DIR_DADAO_SOURCE)
	@rm -fr $(LLVM_TESTSUITE_SOURCE)
	# Clone local repo
	@git clone -q $(LLVM_TESTSUITE_LOCAL) -- $(LLVM_TESTSUITE_SOURCE)
	# Specify version
	@cd $(LLVM_TESTSUITE_SOURCE); git checkout -qb $(LLVM_TESTSUITE_BRANCH) $(LLVM_TESTSUITE_VERSION)
	# New files
	@cp  $(LLVM_TESTSUITE_NEWFILES)/* $(LLVM_TESTSUITE_SOURCE)
	# Patches
	@cd $(LLVM_TESTSUITE_SOURCE); test ! -d $(LLVM_TESTSUITE_PATCHES) || git am $(LLVM_TESTSUITE_PATCHES)/*.patch

testset-llvm-testsuite-build-new:
	@rm -fr $(LLVM_TESTSUITE_BUILD)
	@mkdir -p $(LLVM_TESTSUITE_BUILD)
	# NOTE: $(LLVM_TESTSUITE_SOURCE)/build is softlink to $(LLVM_TESTSUITE_BUILD)
	@ln -s $(LLVM_TESTSUITE_BUILD) $(LLVM_TESTSUITE_SOURCE)/build

testset-llvm-testsuite-build:
	@cmake -S $(LLVM_TESTSUITE_SOURCE) -B $(LLVM_TESTSUITE_BUILD) \
		-DTEST_SUITE_USER_MODE_EMULATION=True\
		-DCMAKE_BUILD_TYPE=Debug\
		-DCMAKE_C_FLAGS_DEBUG='-O0'\
		-DCMAKE_CXX_FLAGS_DEBUG='-O0'\
		-DCMAKE_CXX_COMPILER_WORKS=1\
		-DCMAKE_SIZEOF_VOID_P=8\
		-DCMAKE_SIZEOF_UNSIGNED_INT=4\
		-DCMAKE_C_COMPILER='$(LLVM_1600_BUILD)/bin/clang;-v;-mllvm --optimize-regalloc;-mllvm --regalloc=greedy;-target dadao;-B$(DIR_DADAO_INSTALL)/bin/dadao-unknown-elf-'\
		-DTEST_SUITE_SUBDIRS=SingleSource\
		-DENDIAN=little\
		-DCMAKE_C_FLAGS="-I$(DIR_DADAO_INSTALL)/dadao-unknown-elf/include/ -D__DADAO__=1"\
		-DLINK_OPTIONS='--defsym;__.DADAO.start..text=0x400000'

testset-llvm-testsuite-runtest:
	@cd $(LLVM_TESTSUITE_SOURCE); DIR_DADAO_TOP=$(DIR_DADAO_TOP) ./run-testsuite.py


testset-llvm-testsuite-highfive:
	@echo "BEGIN TO BUILD llvm-testsuite                    at `date +%T`"
	@echo "0. Remove old logfiles                           at `date +%T`"
	@test -d $(DIR_DADAO_LOG) || mkdir -p $(DIR_DADAO_LOG)
	@rm -fr $(LLVM_TESTSUITE_LOG_STDOUT) $(LLVM_TESTSUITE_LOG_STDERR)
	@echo "1. Clean old dirs                                at `date +%T`"
	@make -s testset-llvm-testsuite-clean					1>> $(LLVM_TESTSUITE_LOG_STDOUT) 2>> $(LLVM_TESTSUITE_LOG_STDERR)
	@echo "2. New source                                    at `date +%T`"
	@make -s testset-llvm-testsuite-source					1>> $(LLVM_TESTSUITE_LOG_STDOUT) 2>> $(LLVM_TESTSUITE_LOG_STDERR)
	@echo "3. Configure                                     at `date +%T`"
	@make -s testset-llvm-testsuite-build-new				1>> $(LLVM_TESTSUITE_LOG_STDOUT) 2>> $(LLVM_TESTSUITE_LOG_STDERR)
	@echo "4. Build                                         at `date +%T`"
	@make -s testset-llvm-testsuite-build					1>> $(LLVM_TESTSUITE_LOG_STDOUT) 2>> $(LLVM_TESTSUITE_LOG_STDERR)
	@echo "5. Runtest                                       at `date +%T`"
	@make -s testset-llvm-testsuite-runtest					1>> $(LLVM_TESTSUITE_LOG_STDOUT) 2>> $(LLVM_TESTSUITE_LOG_STDERR)
	@echo "BUILD llvm-testsuite DONE!                       at `date +%T`"
