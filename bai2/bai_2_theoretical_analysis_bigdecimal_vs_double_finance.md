# BÁO CÁO PHÂN TÍCH LÝ THUYẾT: TẠI SAO KHÔNG SỬ DỤNG DOUBLE/FLOAT TRONG BÀI TOÁN TÀI CHÍNH VÀ ĐỊNH GIÁ TOKEN LLM

---

## 1. TỔNG QUAN VÀ ĐẶT VẤN ĐỀ

Trong hệ thống tài chính **RikkeiPay Assistant**, việc kiểm toán chi phí Token tiêu thụ từ các mô hình ngôn ngữ lớn (LLMs như `gemini-2.5-flash` qua OpenRouter) đòi hỏi độ chính xác tuyệt đối ở cấp độ vi phân (micro-cents). 

Đơn giá Token LLM hiện nay cực kỳ nhỏ. Ví dụ:
- Input: `$0.075 / 1,000,000` tokens $ightarrow$ `$0.000000075` cho mỗi token.
- Output: `$0.300 / 1,000,000` tokens $ightarrow$ `$0.0000003` cho mỗi token.

Khi nhân đơn giá này với hàng triệu request mỗi ngày qua các môi trường `dev`, `staging`, `prod` và tổng hợp theo từng phòng ban (`department`), việc lựa chọn sai kiểu dữ liệu số học trong Java sẽ dẫn đến **sai lệch số liệu kế toán**, vi phạm tính toàn vẹn dữ liệu tài chính (**Financial Data Integrity**), và gây thất thoát hoặc sai lệch báo cáo kiểm toán ngân hàng.

---

## 2. NGUYÊN NHÂN GỐC RỄ: CHUẨN SỐ THỰC DẤU PHẨY ĐỘNG IEEE 754

### 2.1. Bản chất biểu diễn nhị phân của số thực dấu phẩy động
Kiểu dữ liệu `float` (32-bit Single Precision) và `double` (64-bit Double Precision) trong Java tuân theo tiêu chuẩn **IEEE 754**. Theo chuẩn này, một số thực $V$ được biểu diễn dưới dạng:

$$V = (-1)^{	ext{sign}} 	imes 	ext{significand (mantissa)} 	imes 2^{	ext{exponent}}$$

Trong hệ thống máy tính:
- Các phân số thập phân có mẫu số là lũy thừa của 2 (ví dụ: $0.5 = 1/2$, $0.25 = 1/4$, $0.125 = 1/8$, $0.0625 = 1/16$) có thể được biểu diễn chính xác tuyệt đối dưới dạng nhị phân hữu hạn.
- Ngược lại, các số thập phân có mẫu số không phải lũy thừa của 2 (ví dụ: $0.1 = 1/10$, $0.2 = 1/5$, $0.075$, $0.3$) sẽ trở thành **chuỗi số vô hạn tuần hoàn** trong hệ nhị phân.

#### Minh họa biểu diễn số 0.1 trong hệ nhị phân:
$$0.1_{10} = 0.00011001100110011001100110011..._2$$

Do thanh ghi 64-bit của `double` chỉ dành ra **52 bits cho Mantissa (Significand)**, giá trị nhị phân buộc phải bị **cắt ngắn (truncated) hoặc làm tròn (rounded)**:
- Giá trị thực tế mà máy tính lưu trữ cho `0.1d` là:
  `0.1000000000000000055511151231257827021181583404541015625`

### 2.2. Minh họa lỗi số học kinh điển trong Java
Một lập trình viên Java nếu thực hiện phép tính đơn giản:

```java
public class FloatingPointDemo {
    public static void main(String[] args) {
        double a = 0.1;
        double b = 0.2;
        double sum = a + b;
        
        System.out.println("0.1 + 0.2 = " + sum);
        System.out.println("sum == 0.3: " + (sum == 0.3));
    }
}
```

**Kết quả in ra Console:**
```text
0.1 + 0.2 = 0.30000000000000004
sum == 0.3: false
```

- Phép so sánh logic `(sum == 0.3)` trả về `false`.
- Xuất hiện phần dư `0.00000000000000004` do lỗi sai số làm tròn tích lũy (**Round-off Error Accumulation**).

---

## 3. TÁC ĐỘNG NGUY HIỂM TRONG HỆ THỐNG TÀI CHÍNH VÀ ĐỊNH GIÁ TOKEN

### 3.1. Sai số tích lũy theo cấp số nhân (Accumulated Round-off Error)
Trong hệ thống tài chính xử lý hàng chục triệu giao dịch/tháng:
- Giả sử mỗi giao dịch phát sinh sai số làm tròn chỉ $0.00000000001\$$ (10 picodollars).
- Khi chạy batch reconciliation tổng hợp chi phí cuối tháng cho 50,000,000 traces:
  $$	ext{Tổng sai số} = 50,000,000 	imes \Delta_{	ext{error}}$$
- Hậu quả: Báo cáo tài chính của Core Banking và dữ liệu tổng hợp từ Tracing (Langfuse) **không thể khớp nhau (Unreconciled Discrepancy)**. Trong kiểm toán tài chính (Financial Audit), sai lệch dù chỉ $1\$$ giữa sổ cái (General Ledger) và chi phí thực tế cũng khiến báo cáo bị gắn cờ đỏ (Audit Flag).

### 3.2. Hiện tượng mất ý nghĩa (Catastrophic Cancellation)
Khi thực hiện phép trừ hai số dấu phẩy động gần bằng nhau (ví dụ: trừ số dư khả dụng với chi phí token ước tính), các chữ số có nghĩa quan trọng sẽ bị triệt tiêu, khiến kết quả chỉ còn lại phần sai số ngẫu nhiên của bộ xử lý số học FPU (Floating-Point Unit).

---

## 4. GIẢI PHÁP TIÊU CHUẨN NGÂN HÀNG: SỬ DỤNG `java.math.BigDecimal`

Để xử lý bài toán định giá tài chính, Java cung cấp lớp đối tượng `BigDecimal`.

### 4.1. Cơ chế hoạt động của `BigDecimal`
`BigDecimal` biểu diễn một số thập phân chính xác không giới hạn (Arbitrary-precision signed decimal numbers) bao gồm 2 thành phần:
1. **Unscaled Value (`BigInteger`):** Giá trị nguyên không chia tỉ lệ (lưu trữ chính xác toàn bộ chữ số).
2. **Scale (`int` 32-bit):** Số lượng chữ số nằm sau dấu phẩy thập phân (số mũ cơ số 10: $	ext{unscaledValue} 	imes 10^{-	ext{scale}}$).

Ví dụ: `$0.075` được lưu trữ chính xác với:
- $	ext{Unscaled Value} = 75$
- $	ext{Scale} = 3$
- Giá trị: $75 	imes 10^{-3} = 0.075$ (Không hề phụ thuộc vào nhị phân hay số mũ cơ số 2).

---

## 5. NHỮNG QUY TẮC BẮT BUỘC KHI LẬP TRÌNH TÀI CHÍNH VỚI `BigDecimal`

### Quy tắc 1: Luôn khởi tạo `BigDecimal` bằng `String` hoặc `BigDecimal.valueOf()`, TUYỆT ĐỐI KHÔNG dùng `new BigDecimal(double)`

| Cú pháp | Kết quả thực tế | Đánh giá |
| :--- | :--- | :--- |
| `new BigDecimal(0.1)` | `0.1000000000000000055511151231257827021181583404541015625` | ❌ **Nghiêm cấm** (Kế thừa lỗi nhị phân của `double`) |
| `new BigDecimal("0.1")` | `0.1` |  **Khuyên dùng** |
| `BigDecimal.valueOf(0.1)` | `0.1` (Tự động gọi `Double.toString(0.1)`) |  **Chấp nhận được** |

### Quy tắc 2: Luôn chỉ định rõ ràng `Scale` và `RoundingMode` trong phép chia (`divide`)
Trong toán học thập phân, phép chia có thể sinh ra số thập phân vô hạn tuần hoàn (ví dụ: $1/3 = 0.33333...$ hoặc $0.075 / 1,000,000$).
Nếu gọi `divide()` mà không chỉ định `Scale` và `RoundingMode`, JVM sẽ lập tức ném ra ngoại lệ:
```text
java.lang.ArithmeticException: Non-terminating decimal expansion; no exact representable decimal result.
```

**Cách triển khai an toàn chuẩn ngân hàng:**
```java
BigDecimal unitPrice = costPerMillion
    .divide(new BigDecimal("1000000"), 16, RoundingMode.HALF_UP);
```

### Quy tắc 3: Sử dụng `RoundingMode.HALF_UP` cho bài toán kế toán chuẩn
- `RoundingMode.HALF_UP` (Làm tròn kiểu học sinh/thương mại: $\ge 0.5$ thì làm tròn lên, $< 0.5$ thì làm tròn xuống) là chuẩn mực được quy định bởi hầu hết các chuẩn mực kế toán (GAAP, IFRS).

---

## 6. THIẾT KẾ TRIỂN KHAI CHO RIKKEIPAY ASSISTANT

### 6.1. Bảng giá Token mô hình `gemini-2.5-flash`
- Input Token: `$0.075 / 1,000,000` tokens
- Output Token: `$0.300 / 1,000,000` tokens

### 6.2. Mã nguồn mẫu chuẩn hóa (`LlmCostCalculator.java`)

```java
package com.rikkeipay.assistant.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class LlmCostCalculator {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    
    // Đơn giá cho 1M Tokens của gemini-2.5-flash
    private static final BigDecimal INPUT_RATE_PER_MILLION = new BigDecimal("0.075");
    private static final BigDecimal OUTPUT_RATE_PER_MILLION = new BigDecimal("0.300");
    
    // Scale trung gian cho tính toán vi phân (16 chữ số sau dấu phẩy)
    private static final int CALCULATION_SCALE = 16;
    // Scale hiển thị kiểm toán (8 chữ số sau dấu phẩy)
    private static final int AUDIT_DISPLAY_SCALE = 8;

    private LlmCostCalculator() {
        // Utility class private constructor
    }

    public static BigDecimal calculateCost(long inputTokens, long outputTokens, String model) {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("Token count cannot be negative");
        }

        // Tính đơn giá cho 1 token (scale = 16)
        BigDecimal singleInputCost = INPUT_RATE_PER_MILLION.divide(ONE_MILLION, CALCULATION_SCALE, RoundingMode.HALF_UP);
        BigDecimal singleOutputCost = OUTPUT_RATE_PER_MILLION.divide(ONE_MILLION, CALCULATION_SCALE, RoundingMode.HALF_UP);

        // Nhân số lượng token với đơn giá
        BigDecimal totalInputCost = singleInputCost.multiply(BigDecimal.valueOf(inputTokens));
        BigDecimal totalOutputCost = singleOutputCost.multiply(BigDecimal.valueOf(outputTokens));

        // Tổng chi phí và làm tròn ở scale kiểm toán (8 chữ số thập phân)
        return totalInputCost.add(totalOutputCost).setScale(AUDIT_DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    public static String formatCost(BigDecimal cost) {
        if (cost == null) {
            return "$0.00000000";
        }
        DecimalFormat df = new DecimalFormat("$0.00000000");
        return df.format(cost);
    }
}
```

---

## 7. BẢNG SO SÁNH TỔNG KẾT

| Đặc tính | `float` / `double` | `BigDecimal` |
| :--- | :--- | :--- |
| **Cơ số biểu diễn** | Nhị phân (Cơ số 2) | Thập phân (Cơ số 10) |
| **Độ chính xác** | Xấp xỉ (Inexact / Approximate) | Chính xác tuyệt đối (Exact Arbitrary Precision) |
| **Lỗi $0.1 + 0.2$** | $= 0.30000000000000004$ | $= 0.3$ |
| **Tốc độ thực thi** | Cực nhanh (xử lý trực tiếp bởi Hardware FPU) | Chậm hơn (xử lý qua đối tượng Java Heap) |
| **Bộ nhớ (Memory footprint)** | Nhẹ (4 bytes / 8 bytes nguyên thủy) | Nặng hơn (Heap Object Allocation) |
| **Ứng dụng phù hợp** | Đồ họa 3D, Game Physics, Machine Learning weights, Tín hiệu số | **Hệ thống Ngân hàng, Sàn chứng khoán, Kiểm toán chi phí LLMOps, Báo cáo thuế** |

---

## 8. KẾT LUẬN

Trong các hệ thống AI tích hợp tài chính như **RikkeiPay Assistant**, chi phí tính toán Token không chỉ là một chỉ số giám sát kỹ thuật mà là một **giao dịch tài chính thực tế**. 

Việc loại bỏ hoàn toàn `double`/`float` và chuẩn hóa sang `BigDecimal` kết hợp cùng chế độ làm tròn `RoundingMode.HALF_UP` là quy chuẩn bắt buộc nhằm:
1. Đảm bảo tính toán độc lập, chính xác tuyệt đối tới cấp độ vi phân token.
2. Triệt tiêu 100% rủi ro sai lệch số liệu kế toán do chuẩn nhị phân IEEE 754.
3. Đáp ứng toàn diện các tiêu chuẩn kiểm toán và quy chế tài chính của doanh nghiệp.


## 9. MINH CHỨNG LOGS

> Task :compileJava
> Task :processResources
> Task :classes
> Task :compileTestJava
> Task :processTestResources NO-SOURCE
> Task :testClasses
> Task :test
BUILD SUCCESSFUL in 31s
4 actionable tasks: 4 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.5.1/userguide/configuration_cache_enabling.html
1:50:04 CH: Execution finished ':test --tests "com.example.bai2.LlmCostCalculatorTest"'.

