/*
 * tc-dadao.h -- Header file for tc-dadao.c.
 * Copyright (C) 2019-2033 Guan Xuetao (AT) Peking Univ.
 *
 * Contributed by:
 *   2019:
 *	Guan Xuetao <gxt@pku.edu.cn>
 */
#ifndef TC_DADAO
#define TC_DADAO

/* See gas/doc/internals.texi for explanation of these macros.  */
#define TARGET_FORMAT "elf64-dadao"
#define TARGET_ARCH bfd_arch_dadao
#define TARGET_BYTES_BIG_ENDIAN 1

extern const char dadao_symbol_chars[];
#define tc_symbol_chars dadao_symbol_chars

/* "@" is a synonym for ".".  */
#define LEX_AT (LEX_BEGIN_NAME)

extern int dadao_label_without_colon_this_line (void);
#define LABELS_WITHOUT_COLONS dadao_label_without_colon_this_line ()

extern int dadao_next_semicolon_is_eoln;
#define TC_EOL_IN_INSN(p) (*(p) == ';' && ! dadao_next_semicolon_is_eoln)

/* This is one direction we can get dadaoal compatibility.  */
extern void dadao_handle_dadaoal (void);
#define md_start_line_hook dadao_handle_dadaoal

extern void dadao_md_begin (void);
#define md_begin dadao_md_begin

extern void dadao_md_end (void);
#define md_end dadao_md_end

extern int dadao_current_location \
  (void (*fn) (expressionS *), expressionS *);
extern int dadao_parse_predefined_name (char *, expressionS *);

extern char *dadao_current_prefix;

/* A bit ugly, since we "know" that there's a static function
   current_location that does what we want.  We also strip off a leading
   ':' in another ugly way.

   The [DVWIOUZX]_Handler symbols are provided when-used.  */

extern int dadao_gnu_syntax;
#define md_parse_name(name, exp, mode, cpos)			\
 (! dadao_gnu_syntax						\
  && (name[0] == '@'						\
      ? (! is_part_of_name (name[1])				\
	 && dadao_current_location (current_location, exp))	\
      : ((name[0] == ':' || ISUPPER (name[0]))			\
	 && dadao_parse_predefined_name (name, exp))))

extern char *dadao_prefix_name (char *);

/* We implement when *creating* a symbol, we also need to strip a ':' or
   prepend a prefix.  */
#define tc_canonicalize_symbol_name(x) \
 (dadao_current_prefix == NULL && (x)[0] != ':' ? (x) : dadao_prefix_name (x))

#define md_undefined_symbol(x) NULL

extern void dadao_fb_label (expressionS *);

/* Since integer_constant is local to expr.c, we have to make this a
   macro.  FIXME: Do it cleaner.  */
#define md_operand(exp)							\
  do									\
    {									\
      if (input_line_pointer[0] == '#')					\
	{								\
	  input_line_pointer++;						\
	  integer_constant (16, (exp));					\
	}								\
      else if (input_line_pointer[0] == '&'				\
	       && input_line_pointer[1] != '&')				\
	as_bad (_("`&' serial number operator is not supported"));	\
      else								\
	dadao_fb_label (exp);						\
    }									\
  while (0)

#define md_number_to_chars number_to_chars_bigendian

#define WORKING_DOT_WORD

extern const struct relax_type dadao_relax_table[];
#define TC_GENERIC_RELAX_TABLE dadao_relax_table

/* We use the relax table for everything except the GREG frags and PUSHJ.  */
extern long dadao_md_relax_frag (segT, fragS *, long);
#define md_relax_frag dadao_md_relax_frag

#define tc_fix_adjustable(FIX)					\
 (((FIX)->fx_addsy == NULL					\
   || S_GET_SEGMENT ((FIX)->fx_addsy) != reg_section)		\
  && (FIX)->fx_r_type != BFD_RELOC_VTABLE_INHERIT		\
  && (FIX)->fx_r_type != BFD_RELOC_VTABLE_ENTRY			\
  && (FIX)->fx_r_type != BFD_RELOC_DADAO_LOCAL)

/* Adjust symbols which are registers.  */
#define tc_adjust_symtab() dadao_adjust_symtab ()
extern void dadao_adjust_symtab (void);

/* Here's where we make all symbols global, when so requested.
   We must avoid doing that for expression symbols or section symbols,
   though.  */
extern int dadao_globalize_symbols;
#define tc_frob_symbol(sym, punt)				\
  do								\
    {								\
      if (S_GET_SEGMENT (sym) == reg_section)			\
	{							\
	  if (S_GET_NAME (sym)[0] != '$'			\
	      && S_GET_VALUE (sym) < 256)			\
	    {							\
	      if (dadao_globalize_symbols)			\
		S_SET_EXTERNAL (sym);				\
	      else						\
		symbol_mark_used_in_reloc (sym);		\
	    }							\
	}							\
      else if (dadao_globalize_symbols				\
	       && ! symbol_section_p (sym)			\
	       && sym != section_symbol (absolute_section)	\
	       && ! S_IS_LOCAL (sym))				\
	S_SET_EXTERNAL (sym);					\
    }								\
  while (0)

/* No shared lib support, so we don't need to ensure externally
   visible symbols can be overridden.  */
#define EXTERN_FORCE_RELOC 0

/* When relaxing, we need to emit various relocs we otherwise wouldn't.  */
#define TC_FORCE_RELOCATION(fix) dadao_force_relocation (fix)
extern int dadao_force_relocation (struct fix *);

/* Call md_pcrel_from_section(), not md_pcrel_from().  */
#define MD_PCREL_FROM_SECTION(FIX, SEC) md_pcrel_from_section (FIX, SEC)
extern long md_pcrel_from_section (struct fix *, segT);

#define md_section_align(seg, size) (size)

#define LISTING_HEADER "GAS for DADAO"

/* The default of 4 means Bcc expansion looks like it's missing a line.  */
#define LISTING_LHS_CONT_LINES 5

extern fragS *dadao_opcode_frag;
#define TC_FRAG_TYPE fragS *
#define TC_FRAG_INIT(frag, max_bytes) (frag)->tc_frag_data = dadao_opcode_frag

/* We need to associate each section symbol with a list of GREGs defined
   for that section/segment and sorted on offset, between the point where
   all symbols have been evaluated and all frags mapped, and when the
   fixups are done and relocs are output.  Similarly for each unknown
   symbol.  */
extern void dadao_frob_file (void);
#define tc_frob_file_before_fix()					\
  do									\
    {									\
      int i = 0;							\
									\
      /* It's likely dadao_frob_file changed (removed) sections, so make	\
	 sure sections are correctly numbered as per renumber_sections,	\
	 (static to write.c where this macro is called).  */		\
      dadao_frob_file ();						\
      bfd_map_over_sections (stdoutput, renumber_sections, &i);		\
    }									\
  while (0)

/* Used by dadao_frob_file.  Hangs on section symbols and unknown symbols.  */
struct dadao_symbol_gregs;
#define TC_SYMFIELD_TYPE struct dadao_symbol_gregs *

/* Used by relaxation, counting maximum needed PUSHJ stubs for a section.  */
struct dadao_segment_info_type
 {
   /* We only need to keep track of the last stubbable frag because
      there's no less hackish way to keep track of different relaxation
      rounds.  */
   fragS *last_stubfrag;
   bfd_size_type nstubs;
 };
#define TC_SEGMENT_INFO_TYPE struct dadao_segment_info_type

extern void dadao_md_elf_section_change_hook (void);
#define md_elf_section_change_hook dadao_md_elf_section_change_hook

extern void dadao_md_do_align (int, char *, int, int);
#define md_do_align(n, fill, len, max, label) \
 dadao_md_do_align (n, fill, len, max)

/* Each insn is a tetrabyte (4 bytes) long, but if there are BYTE
   sequences sprinkled in, we can get unaligned DWARF2 offsets, so let's
   explicitly say one byte.  */
#define DWARF2_LINE_MIN_INSN_LENGTH 1

/* This target is buggy, and sets fix size too large.  */
#define TC_FX_SIZE_SLACK(FIX) 6

/* DADAO has global register symbols.  */
#define TC_GLOBAL_REGISTER_SYMBOL_OK
#ifndef TARGET_BYTES_BIG_ENDIAN
#define TARGET_BYTES_BIG_ENDIAN			1
#endif

#endif /* TC_DADAO */
