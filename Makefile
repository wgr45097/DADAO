#
# TOP Makefile
#
DIR_GIT_TAO		:= /pub/GIT-TAO/

DIR_DADAO		:= $(shell pwd)
DIR_DADAO_SOURCE	:= $(DIR_DADAO)/__source/
DIR_DADAO_BUILD		:= $(DIR_DADAO)/__build/
DIR_DADAO_INSTALL	:= $(DIR_DADAO)/__install/

all:
	@echo ""
	@echo "DA DAO ZHI JIAN!"
	@echo ""

include DADAO-tch/Makefile.DADAO-tch
include DADAO-sim/Makefile.DADAO-sim
include DADAO-env/Makefile.DADAO-env

DADAO-highfive:
	@make --silent DADAO-clean
	@echo "BEGIN TO BUILD EVERYTHING!"
	@make --silent tch-highfive
	@echo "BUILD EVERYTHING DONE!"

DADAO-clean:
	@echo "CLEAR EVERYTHING!"
	@rm -fr $(DIR_DADAO_SOURCE)
	@rm -fr $(DIR_DADAO_BUILD)
	@rm -fr $(DIR_DADAO_INSTALL)
	@rm -fr _log-std*
	@echo "CLEAR EVERYTHING DONE!"
