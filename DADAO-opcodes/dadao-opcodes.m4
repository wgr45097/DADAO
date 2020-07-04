#	$1		$2	$3	$4	$5	$6	$7	$8	$9
#	name		op-type	maj-op	min-op	fa	fb	fc	fd	insn-type
divert(0)dnl

insn(	nop,		o000,	0x00,	0x00,	op,	-,	-,	-,	normal)
insn(	mov.rv,		orr0,	0x00,	0x11,	op,	rv,	rv,	-,	normal)
insn(	put.rv,		orr0,	0x00,	0x13,	op,	rv,	rg,	-,	normal)
insn(	get.rv,		orr0,	0x00,	0x15,	op,	rg,	rv,	-,	normal)

insn(	slu,		orri,	0x00,	0x20,	op,	rg,	rg,	u6,	normal)
insn(  _slu,		orrr,	0x00,	0x21,	op,	rg,	rg,	rg,	normal)
insn(	sr,		orri,	0x00,	0x22,	op,	rg,	rg,	u6,	normal)
insn(  _sr,		orrr,	0x00,	0x23,	op,	rg,	rg,	rg,	normal)
insn(	sru,		orri,	0x00,	0x24,	op,	rg,	rg,	u6,	normal)
insn(  _sru,		orrr,	0x00,	0x25,	op,	rg,	rg,	rg,	normal)
insn(	not,		orri,	0x00,	0x26,	op,	rg,	rg,	u6,	normal)
insn(  _not,		orrr,	0x00,	0x27,	op,	rg,	rg,	rg,	normal)

insn(	mul,		rrii,	0x02,	0,	rg,	rg,	s12,	-,	normal)
insn(  _mul,		rrri,	0x03,	0,	rg,	rg,	rg,	i6,	normal)

insn(	mulu,		rrrr,	0x05,	0,	rg,	rg,	rg,	rg,	normal)
insn(	div,		rrrr,	0x06,	0,	rg,	rg,	rg,	rg,	normal)
insn(	divu,		rrrr,	0x07,	0,	rg,	rg,	rg,	rg,	normal)

insn(	or,		rrii,	0x10,	0,	rg,	rg,	u12,	-,	normal)
insn(  _or,		rrri,	0x11,	0,	rg,	rg,	rg,	i6,	normal)
insn(	and,		rrii,	0x12,	0,	rg,	rg,	u12,	-,	normal)
insn(  _and,		rrri,	0x13,	0,	rg,	rg,	rg,	i6,	normal)
insn(	xor,		rrii,	0x14,	0,	rg,	rg,	u12,	-,	normal)
insn(  _xor,		rrri,	0x15,	0,	rg,	rg,	rg,	i6,	normal)

insn(	setwl,		rjii,	0x16,	0,	rg,	w16,	-,	-,	normal)
insn(	setwk,		rjii,	0x16,	1,	rg,	w16,	-,	-,	normal)
insn(	setwj,		rjii,	0x16,	2,	rg,	w16,	-,	-,	normal)
insn(	setwh,		rjii,	0x16,	3,	rg,	w16,	-,	-,	normal)
insn(	incwl,		rjii,	0x17,	0,	rg,	w16,	-,	-,	normal)
insn(	incwk,		rjii,	0x17,	1,	rg,	w16,	-,	-,	normal)
insn(	incwj,		rjii,	0x17,	2,	rg,	w16,	-,	-,	normal)
insn(	incwh,		rjii,	0x17,	3,	rg,	w16,	-,	-,	normal)

insn(	add,		rrii,	0x20,	0,	rg,	rg,	s12,	-,	normal)
insn(  _add,		rrri,	0x21,	0,	rg,	rg,	rg,	i6,	normal)
insn(	addu,		rrii,	0x22,	0,	rg,	rg,	u12,	-,	normal)
insn(  _addu,		rrri,	0x23,	0,	rg,	rg,	rg,	i6,	normal)
insn(	sub,		rrii,	0x24,	0,	rg,	rg,	s12,	-,	normal)
insn(  _sub,		rrri,	0x25,	0,	rg,	rg,	rg,	i6,	normal)
insn(	subu,		rrii,	0x26,	0,	rg,	rg,	u12,	-,	normal)
insn(  _subu,		rrri,	0x27,	0,	rg,	rg,	rg,	i6,	normal)

insn(	cs_n,		rrrr,	0x46,	0,	rg,	rg,	rg,	rg,	normal)
insn(	cs_nn,		rrrr,	0x47,	0,	rg,	rg,	rg,	rg,	normal)
insn(	cs_z,		rrrr,	0x56,	0,	rg,	rg,	rg,	rg,	normal)
insn(	cs_nz,		rrrr,	0x57,	0,	rg,	rg,	rg,	rg,	normal)
insn(	cs_p,		rrrr,	0x66,	0,	rg,	rg,	rg,	rg,	normal)
insn(	cs_np,		rrrr,	0x67,	0,	rg,	rg,	rg,	rg,	normal)
insn(	cs_od,		rrrr,	0x76,	0,	rg,	rg,	rg,	rg,	normal)
insn(	cs_ev,		rrrr,	0x77,	0,	rg,	rg,	rg,	rg,	normal)

insn(	cmp,		rrii,	0x70,	0,	rg,	rg,	s12,	-,	normal)
insn(  _cmp,		rrri,	0x71,	0,	rg,	rg,	rg,	i6,	normal)
insn(	cmpu,		rrii,	0x72,	0,	rg,	rg,	u12,	-,	normal)
insn(  _cmpu,		rrri,	0x73,	0,	rg,	rg,	rg,	i6,	normal)

insn(	ldb,		rrii,	0x80,	0,	rg,	rp,	s12,	-,	dref)
insn(  _ldb,		rrri,	0x81,	0,	rg,	rp,	rg,	i6,	dref)
insn(	ldw,		rrii,	0x82,	0,	rg,	rp,	s12,	-,	dref)
insn(  _ldw,		rrri,	0x83,	0,	rg,	rp,	rg,	i6,	dref)
insn(	ldt,		rrii,	0x84,	0,	rg,	rp,	s12,	-,	dref)
insn(  _ldt,		rrri,	0x85,	0,	rg,	rp,	rg,	i6,	dref)
insn(	ldo,		rrii,	0x86,	0,	rg,	rp,	s12,	-,	dref)
insn(  _ldo,		rrri,	0x87,	0,	rg,	rp,	rg,	i6,	dref)
insn(	ldbu,		rrii,	0x90,	0,	rg,	rp,	s12,	-,	dref)
insn(  _ldbu,		rrri,	0x91,	0,	rg,	rp,	rg,	i6,	dref)
insn(	ldwu,		rrii,	0x92,	0,	rg,	rp,	s12,	-,	dref)
insn(  _ldwu,		rrri,	0x93,	0,	rg,	rp,	rg,	i6,	dref)
insn(	ldtu,		rrii,	0x94,	0,	rg,	rp,	s12,	-,	dref)
insn(  _ldtu,		rrri,	0x95,	0,	rg,	rp,	rg,	i6,	dref)
insn(	stb,		rrii,	0xA0,	0,	rg,	rp,	s12,	-,	dref)
insn(  _stb,		rrri,	0xA1,	0,	rg,	rp,	rg,	i6,	dref)
insn(	stw,		rrii,	0xA2,	0,	rg,	rp,	s12,	-,	dref)
insn(  _stw,		rrri,	0xA3,	0,	rg,	rp,	rg,	i6,	dref)
insn(	stt,		rrii,	0xA4,	0,	rg,	rp,	s12,	-,	dref)
insn(  _stt,		rrri,	0xA5,	0,	rg,	rp,	rg,	i6,	dref)
insn(	sto,		rrii,	0xA6,	0,	rg,	rp,	s12,	-,	dref)
insn(  _sto,		rrri,	0xA7,	0,	rg,	rp,	rg,	i6,	dref)

insn(	br_n,		riii,	0x48,	0,	rg,	s18,	-,	-,	condbranch)
insn(	br_nn,		riii,	0x49,	0,	rg,	s18,	-,	-,	condbranch)
insn(	br_z,		riii,	0x58,	0,	rg,	s18,	-,	-,	condbranch)
insn(	br_nz,		riii,	0x59,	0,	rg,	s18,	-,	-,	condbranch)
insn(	br_p,		riii,	0x68,	0,	rg,	s18,	-,	-,	condbranch)
insn(	br_np,		riii,	0x69,	0,	rg,	s18,	-,	-,	condbranch)
insn(	br_od,		riii,	0x78,	0,	rg,	s18,	-,	-,	condbranch)
insn(	br_ev,		riii,	0x79,	0,	rg,	s18,	-,	-,	condbranch)

insn(	rp_p2g,		orr0,	0x08,	0x00,	op,	rg,	rp,	-,	normal)
insn(	rp_g2p,		orr0,	0x08,	0x01,	op,	rp,	rg,	-,	normal)
insn(	rp_p2p,		orr0,	0x08,	0x02,	op,	rp,	rp,	-,	normal)

insn(	ret,		o000,	0x08,	0x37,	op,	-,	-,	-,	jsr)
insn(	trip,		oiii,	0x08,	0x38,	op,	u18,	-,	-,	jsr)
insn(	trap,		oiii,	0x08,	0x3F,	op,	u18,	-,	-,	jsr)

insn(	geta,		riii,	0x18,	0,	rg,	s18,	-,	-,	geta)
insn(	rp_add,		rrii,	0x28,	0,	rp,	rp,	s12,	-,	normal)
insn(  _rp_add,		rrri,	0x29,	0,	rp,	rp,	rg,	i6,	normal)
insn(	rp_ldo,		rrii,	0x88,	0,	rp,	rp,	s12,	-,	dref)
insn(  _rp_ldo,		rrri,	0x89,	0,	rp,	rp,	rg,	i6,	dref)
insn(	rp_sto,		rrii,	0xA8,	0,	rp,	rp,	s12,	-,	dref)
insn(  _rp_sto,		rrri,	0xA9,	0,	rp,	rp,	rg,	i6,	dref)
insn(	call,		iiii,	0xC8,	0,	s24,	-,	-,	-,	jsr)
insn(  _call,		rrii,	0xC9,	0,	rg,	rp,	s12,	-,	jsr)
insn(	jump,		iiii,	0xD8,	0,	s24,	-,	-,	-,	branch)
insn(  _jump,		rrii,	0xD9,	0,	rg,	rp,	s12,	-,	branch)

insn(	fcmp,		orrr,	0xDB,	1,	op,	rg,	rf,	rf,	normal)
insn(	fun,		orrr,	0xDB,	2,	op,	rg,	rf,	rf,	normal)
insn(	feql,		orrr,	0xDB,	3,	op,	rg,	rf,	rf,	normal)
insn(	fcmpe,		orrr,	0xDB,	17,	op,	rg,	rf,	rf,	normal)
insn(	fune,		orrr,	0xDB,	18,	op,	rg,	rf,	rf,	normal)
insn(	feqle,		orrr,	0xDB,	19,	op,	rg,	rf,	rf,	normal)

insn(	ft_get,		orr0,	0x0A,	0x00,	op,	rg,	rf,	-,	normal)
insn(	ft_put,		orr0,	0x0A,	0x01,	op,	rf,	rg,	-,	normal)
insn(	ft_2ft,		orr0,	0x0A,	0x02,	op,	rf,	rf,	-,	normal)
insn(	ft_2fo,		orr0,	0x0A,	0x03,	op,	rf,	rf,	-,	normal)
insn(	ft_i2f,		orr0,	0x0A,	0x04,	op,	rg,	rf,	-,	normal)
insn(	ft_u2f,		orr0,	0x0A,	0x05,	op,	rg,	rf,	-,	normal)
insn(	ft_f2i,		orr0,	0x0A,	0x06,	op,	rf,	rg,	-,	normal)
insn(	ft_f2u,		orr0,	0x0A,	0x07,	op,	rf,	rg,	-,	normal)

insn(	ftc_un,		orrr,	0x0A,	0x08,	op,	rg,	rf,	rf,	normal)
insn(	ftc_or,		orrr,	0x0A,	0x09,	op,	rg,	rf,	rf,	normal)
insn(	ftc_ne,		orrr,	0x0A,	0x0A,	op,	rg,	rf,	rf,	normal)
insn(	ftc_eq,		orrr,	0x0A,	0x0B,	op,	rg,	rf,	rf,	normal)
insn(	ftc_lt,		orrr,	0x0A,	0x0C,	op,	rg,	rf,	rf,	normal)
insn(	ftc_ge,		orrr,	0x0A,	0x0D,	op,	rg,	rf,	rf,	normal)
insn(	ftc_gt,		orrr,	0x0A,	0x0E,	op,	rg,	rf,	rf,	normal)
insn(	ftc_le,		orrr,	0x0A,	0x0F,	op,	rg,	rf,	rf,	normal)

insn(	ft_add,		orrr,	0x0A,	0x10,	op,	rf,	rf,	rf,	normal)
insn(	ft_sub,		orrr,	0x0A,	0x11,	op,	rf,	rf,	rf,	normal)
insn(	ft_mul,		orrr,	0x0A,	0x12,	op,	rf,	rf,	rf,	normal)
insn(	ft_div,		orrr,	0x0A,	0x13,	op,	rf,	rf,	rf,	normal)

insn(	ft_abs,		orr0,	0x0A,	0x18,	op,	rf,	rf,	-,	normal)
insn(	ft_neg,		orr0,	0x0A,	0x19,	op,	rf,	rf,	-,	normal)
insn(	ft_sqrt,	orr0,	0x0A,	0x1A,	op,	rf,	rf,	-,	normal)

insn(	fo_get,		orr0,	0x0B,	0x00,	op,	rg,	rf,	-,	normal)
insn(	fo_put,		orr0,	0x0B,	0x01,	op,	rf,	rg,	-,	normal)
insn(	fo_2ft,		orr0,	0x0B,	0x02,	op,	rf,	rf,	-,	normal)
insn(	fo_2fo,		orr0,	0x0B,	0x03,	op,	rf,	rf,	-,	normal)
insn(	fo_i2f,		orr0,	0x0B,	0x04,	op,	rg,	rf,	-,	normal)
insn(	fo_u2f,		orr0,	0x0B,	0x05,	op,	rg,	rf,	-,	normal)
insn(	fo_f2i,		orr0,	0x0B,	0x06,	op,	rf,	rg,	-,	normal)
insn(	fo_f2u,		orr0,	0x0B,	0x07,	op,	rf,	rg,	-,	normal)

insn(	foc_un,		orrr,	0x0B,	0x08,	op,	rg,	rf,	rf,	normal)
insn(	foc_or,		orrr,	0x0B,	0x09,	op,	rg,	rf,	rf,	normal)
insn(	foc_ne,		orrr,	0x0B,	0x0A,	op,	rg,	rf,	rf,	normal)
insn(	foc_eq,		orrr,	0x0B,	0x0B,	op,	rg,	rf,	rf,	normal)
insn(	foc_lt,		orrr,	0x0B,	0x0C,	op,	rg,	rf,	rf,	normal)
insn(	foc_ge,		orrr,	0x0B,	0x0D,	op,	rg,	rf,	rf,	normal)
insn(	foc_gt,		orrr,	0x0B,	0x0E,	op,	rg,	rf,	rf,	normal)
insn(	foc_le,		orrr,	0x0B,	0x0F,	op,	rg,	rf,	rf,	normal)

insn(	fo_add,		orrr,	0x0B,	0x10,	op,	rf,	rf,	rf,	normal)
insn(	fo_sub,		orrr,	0x0B,	0x11,	op,	rf,	rf,	rf,	normal)
insn(	fo_mul,		orrr,	0x0B,	0x12,	op,	rf,	rf,	rf,	normal)
insn(	fo_div,		orrr,	0x0B,	0x13,	op,	rf,	rf,	rf,	normal)

insn(	fo_abs,		orr0,	0x0B,	0x18,	op,	rf,	rf,	-,	normal)
insn(	fo_neg,		orr0,	0x0B,	0x19,	op,	rf,	rf,	-,	normal)
insn(	fo_sqrt,	orr0,	0x0B,	0x1A,	op,	rf,	rf,	-,	normal)

insn(	ft_ld,		rrii,	0x8A,	0,	rf,	rp,	s12,	-,	dref)
insn(  _ft_ld,		rrri,	0x8B,	0,	rf,	rp,	rg,	i6,	dref)
insn(	fo_ld,		rrii,	0x9A,	0,	rf,	rp,	s12,	-,	dref)
insn(  _fo_ld,		rrri,	0x9B,	0,	rf,	rp,	rg,	i6,	dref)
insn(	ft_st,		rrii,	0xAA,	0,	rf,	rp,	s12,	-,	dref)
insn(  _ft_st,		rrri,	0xAB,	0,	rf,	rp,	rg,	i6,	dref)
insn(	fo_st,		rrii,	0xBA,	0,	rf,	rp,	s12,	-,	dref)
insn(  _fo_st,		rrri,	0xBB,	0,	rf,	rp,	rg,	i6,	dref)
