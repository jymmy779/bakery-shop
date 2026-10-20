# Hướng dẫn dành cho AI coding agent

File này quy định cách AI hỗ trợ thành viên làm việc trong dự án Bakery Shop.
Trước khi sửa code, phải đọc:

- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/USE_CASE_ASSIGNMENT.md`
- `docs/PROJECT_PROGRESS.md`

## Phạm vi công việc

- Chỉ sửa các package, template và use case thuộc phạm vi của thành viên đang làm.
- Không tự ý sửa Entity, Enum, service interface, `SecurityConfig`, `pom.xml` hoặc
  `application*.properties` nếu thay đổi đó chưa được người dùng yêu cầu.
- Controller chỉ nhận request và gọi service; không đặt business logic trong
  controller và không gọi repository trực tiếp.
- Business logic đặt trong service implementation. Repository chỉ phụ trách truy
  xuất dữ liệu.
- Dùng DTO cho dữ liệu request/response; không nhận Entity trực tiếp từ form.
- Không commit mật khẩu, App Password, API key, dữ liệu MySQL local, `.env`,
  `.idea`, `target` hoặc `uploads`.
- Không xóa, hoàn tác hoặc ghi đè thay đổi của thành viên khác nếu chưa được yêu cầu.

## Kiểm tra trước khi kết thúc

Sau khi thay đổi code, AI phải:

1. Kiểm tra các file đã thay đổi và bảo đảm không vượt phạm vi use case.
2. Chạy test liên quan nếu môi trường cho phép.
3. Nếu không thể chạy test, ghi rõ nguyên nhân; không được nói rằng test đã đạt.
4. Kiểm tra project có lỗi compile hay lỗi cấu hình rõ ràng hay không.
5. Tóm tắt phần đã làm, phần chưa làm và các rủi ro còn lại cho người dùng.

## Tự động cập nhật tiến độ

Khi AI đã **thực sự thay đổi code hoặc giao diện cho một use case**, trước khi kết
thúc công việc phải cập nhật `docs/PROJECT_PROGRESS.md` theo các quy tắc sau:

1. Tìm mục `Nhật ký công việc` và ngày hiện tại theo định dạng `DD/MM/YYYY`.
2. Nếu ngày hiện tại chưa tồn tại, tạo tiêu đề ngày và bảng có cùng cấu trúc với
   ngày gần nhất.
3. Thêm hoặc cập nhật đúng một dòng cho phần việc vừa thực hiện, gồm:
   - Thành viên thực hiện.
   - Use case hoặc phần việc.
   - Kết quả cụ thể đã làm được.
   - Trạng thái hiện tại.
   - Branch, PR hoặc commit nếu đã biết.
   - Test hoặc cách đã demo.
   - Việc tiếp theo hay vướng mắc.
4. Chỉ dùng trạng thái đã định nghĩa trong `docs/PROJECT_PROGRESS.md`.
5. Chưa có PR thì ghi tên branch; chưa biết số PR/commit thì ghi `Chưa có` thay vì
   tự đoán.
6. Không ghi test đã đạt nếu test chưa được chạy.
7. Không tự đánh dấu `Hoàn thành` nếu code chưa merge vào `master` hoặc chưa được
   nghiệm thu. Trước khi merge, trạng thái cao nhất thông thường là `Chờ review`.
8. Không sửa hoặc tự chốt phần `Checkpoint sprint`. Phần này do nhóm trưởng tổng
   hợp vào cuối sprint.
9. Không xóa hay viết lại lịch sử của thành viên khác. Nếu kết quả bị thay đổi hoặc
   revert, thêm ghi chú mới để giữ lại lịch sử.

Không cập nhật nhật ký khi người dùng chỉ yêu cầu giải thích, tư vấn, đọc code,
review hoặc kiểm tra mà không tạo ra thay đổi triển khai thực tế.

## Nội dung bàn giao cho Pull Request

Khi người dùng chuẩn bị tạo Pull Request, AI phải cung cấp nội dung gồm:

- Summary một câu.
- Use case đã thực hiện và kết quả cụ thể.
- Cách kiểm thử và kết quả kiểm thử thật sự.
- Phần chưa hoàn thành hoặc lỗi đã biết.
- Xác nhận đã cập nhật `docs/PROJECT_PROGRESS.md`.

AI không tự commit, push, merge hoặc thay đổi nhánh nếu người dùng chưa yêu cầu rõ.
