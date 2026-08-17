
## 1. Bảng so sánh tổng quan

| Tiêu chí so sánh | Spring WebMVC (Đồng bộ / Blocking) | Spring WebFlux (Bất đồng bộ / Non-blocking) |
| :--- | :--- | :--- |
| **Mô hình Thread Pool** | Sử dụng mô hình **Thread-per-request** (Mỗi request chiếm một Thread riêng trong suốt vòng đời xử lý). | Sử dụng mô hình **Event Loop** (Project Reactor - số lượng thread ít, thường tương ứng với số lõi CPU). |
| **Khả năng chịu tải (Concurrency)** | Kém khi có nhiều kết nối giữ lâu. Nếu 200 client cùng gọi API stream kéo dài 10 giây, toàn bộ 200 Thread sẽ bị khóa cứng, dễ dẫn đến cạn kiệt Thread Pool (`Thread Exhaustion`). | Cực kỳ cao. Không có thread nào bị block trong lúc chờ dữ liệu từ OpenAI/OpenRouter trả về từng token. Một vài thread có thể gánh hàng nghìn kết nối đồng thời. |
| **Cơ chế trả dữ liệu (Streaming)** | Phải dùng `SseEmitter` hoặc `ResponseBodyEmitter` viết khá phức tạp, thread vẫn phải giữ kết nối và chịu áp lực bộ nhớ đệm (buffer). | Hỗ trợ natively thông qua `Flux<ServerSentEvent>` hoặc `Flux<String>`, dữ liệu đẩy trực tiếp từ TCP socket của AI tới client ngay khi có token mới. |
| **Tài nguyên hệ thống** | Tiêu tốn nhiều RAM và CPU Context Switching do số lượng thread lớn. | Tối ưu hóa tài nguyên phần cứng tối đa, tiết kiệm RAM đáng kể trong các ứng dụng AI-driven quy mô lớn. |

---

## 2. Phân tích chi tiết hành vi xử lý khi tích hợp LLM

### Spring WebMVC (Mô hình truyền thống)
* **Vấn đề nghẽn cổ chai (Blocking I/O):** Trong Spring WebMVC, mỗi HTTP Request đến sẽ được gán cho một Thread từ Tomcat Thread Pool. Khi bạn thực hiện gọi LLM dưới dạng stream, luồng xử lý sẽ bị giữ lại để chờ từng gói tin (chunk/token) từ API bên thứ ba trả về.
* **Hậu quả:** Nếu lượng truy cập đồng thời tăng cao, số lượng request vượt quá giới hạn Thread Pool của server, các request mới sẽ phải xếp hàng chờ (`Thread Starvation`), làm giảm hiệu năng toàn hệ thống và gây lãng phí tài nguyên RAM/CPU.

### Spring WebFlux (Mô hình Reactive)
* **Xử lý bất đồng bộ không nghẽn (Non-blocking I/O):** Dựa trên nền tảng Project Reactor (`Mono`/`Flux`), WebFlux vận hành theo cơ chế hướng sự kiện (Event-driven). Khi một request stream được khởi tạo, thread ban đầu không bị khóa mà được giải phóng ngay lập tức để phục vụ request khác.
* **Cơ chế đẩy dữ liệu (Push-based):** Khi LLM sinh ra một token mới, hệ thống sẽ tự động đẩy (`emit`) token đó xuống client thông qua luồng phản ứng (Reactive Streams) mà không tiêu tốn tài nguyên chờ đợi. Điều này giúp tối ưu hóa tuyệt đối cho các tính năng Chatbot AI, Real-time Logging hoặc Server-Sent Events (SSE).



3. Minh chứng thực tế
   2026-08-17 22:10:00.125  INFO [Session04_demo] --- [nio-8080-exec-1] c.i.IncidentStreamController : Nhận request stream sự cố: messageLength=35, temp=0.4, maxTokens=500
   2026-08-17 22:10:00.450  INFO [Session04_demo] --- [parallel-1]   reactor.Flux.Map.1       : Bắt đầu đẩy dữ liệu SSE token đầu tiên từ OpenRouter model...
   2026-08-17 22:10:04.890  INFO [Session04_demo] --- [parallel-1]   c.i.IncidentStreamController : Hoàn thành streaming phản hồi tới client.