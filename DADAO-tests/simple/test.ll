; ModuleID = 'test.c'
source_filename = "test.c"
target datalayout = "e-m:e-p:64:64-i64:64-a:0:64-n64-S128"
target triple = "dadao"

; Function Attrs: noinline nounwind optnone
define dso_local i32 @main(i32 inreg noundef %argc) #0 {
entry:
  %x = alloca i32, align 4
  %y = alloca i32, align 4
  store i32 3, ptr %x, align 4
;  store i32 4, ptr %y, align 4
  %0 = load i32, ptr %x, align 4
;  %1 = load i32, ptr %y, align 4
  %cmp = icmp slt i32 %0, 4
  %conv = zext i1 %cmp to i32
  ret i32 %conv
}

attributes #0 = { noinline nounwind optnone "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" }

!llvm.module.flags = !{!0, !1}
!llvm.ident = !{!2}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"frame-pointer", i32 2}
!2 = !{!"clang version 16.0.0 (/pub/GITHUB/llvm/llvm-project.git 84d20d3feae0d87ea3329d2d12f61d46be76c2c4)"}
