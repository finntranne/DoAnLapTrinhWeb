let stompClient = null;
let customerId = document.getElementById("customerId").value;
let shopId = document.getElementById("shopId").value;

function connect() {
    const socket = new SockJS("/ws-chat");
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function () {
        console.log("Connected to WebSocket");

        // Đăng ký nhận tin nhắn từ shop
        stompClient.subscribe(`/topic/shop/${shopId}`, function (message) {
            const msg = JSON.parse(message.body);
            // Nếu senderType là CUSTOMER thì mine = true
            const mine = msg.senderType === "CUSTOMER" && msg.customerId == customerId;
            showMessage(msg, mine);
        });
    });
}

// Gửi tin nhắn
function sendMessage() {
    const content = document.getElementById("messageInput").value;
    if (!content.trim()) return;

    const message = {
        customerId: customerId,
        shopId: shopId,
        content: content,
        senderType: "CUSTOMER"
    };

    stompClient.send("/app/sendMessage", {}, JSON.stringify(message));
    // Hiển thị tin nhắn của chính khách hàng ngay lập tức
    showMessage(message, true);
    document.getElementById("messageInput").value = "";
}

// Hiển thị tin nhắn
function showMessage(msg, mine) {
    const chatBox = document.getElementById("chatBox");
    const div = document.createElement("div");
    div.className = mine ? "message mine" : "message theirs";
    div.textContent = (mine ? "Bạn: " : "Shop: ") + msg.content;
    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
}

// Tự động kết nối khi load trang
window.onload = connect;
