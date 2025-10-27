const container = document.getElementById("container");
const registerBtn = document.getElementById("register");
const loginBtn = document.getElementById("login");
const signUpForm = document.getElementById("signupForm");
const signInForm = document.getElementById("signinForm");
const toggle = document.getElementById("toggle");
const loginForm = document.querySelector("signinForm");
const otpContainer = document.getElementById("otpContainer");
const otpInputs = document.querySelectorAll('.otp-input');
const emailInput = document.getElementById('email-input');
const emailDisplay = document.getElementById('email-display');
const emailHidden = document.getElementById('email-hidden');
const resendBtn = document.getElementById('resend-otp');
const resendBtnforgot = document.getElementById('resend-otpforgot');
const otpForm = document.getElementById("otpForm");
const otpForgotForm = document.getElementById("otpForgotForm");
const otpEmail = document.getElementById("otpEmail");
const forgotPasswordForm = document.getElementById("forgotPasswordForm");
const forgotPasswordMessage = document.getElementById("forgotPasswordMessage");
const forgotOtpContainer = document.getElementById("forgotOtpContainer");
const forgotOtpEmail = document.getElementById("forgotOtpEmail");
const forgotPasswordLink = document.getElementById("forgotPasswordLink");
const forgotPasswordContainer = document.getElementById("forgotPasswordContainer");
const signinFormContainer = document.getElementById("signinForm").parentElement;
const cancelForgotPassword = document.getElementById("cancelForgotPassword");


registerBtn.addEventListener("click", () => {
	container.classList.add("active");
});

loginBtn.addEventListener("click", () => {
	container.classList.remove("active");
});

function showMessage(message, type = "success") {
	const box = document.getElementById("messageBox");
	if (!box) return console.warn("Không tìm thấy #messageBox trong HTML");

	box.textContent = message;
	box.className = `message-box ${type}`;
	box.style.display = "block";

	// Tự ẩn sau 3 giây
	setTimeout(() => {
		box.style.opacity = "0";
		setTimeout(() => {
			box.style.display = "none";
			box.style.opacity = "1";
		}, 300);
	}, 3000);
}





signUpForm.addEventListener("submit", async (e) => {
	e.preventDefault();
	const formData = new FormData(signUpForm);
	const data = {
		fullname: formData.get("fullname"),
		username: formData.get("username"),
		email: formData.get("email"),
		password: formData.get("password")
	};

	try {
		const response = await fetch("/auth/signup", {
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			},
			body: JSON.stringify(data)
		});

		if (!response.ok) {
			const errorData = await response.json();
			showMessage("Đăng ký thất bại: " + (errorData.message || "Lỗi server"), "error");
			return;
		}

		signUpForm.parentElement.style.display = "none";
		signInForm.parentElement.style.display = "none";
		toggle.style.display = "none";
		otpContainer.style.display = "flex";

		if (typeof startCountdown === "function") {
			startCountdown();
		}

		document.getElementById("otpEmail").value = formData.get("email");
		showMessage("Đăng ký thành công! Vui lòng nhập mã OTP được gửi đến email của bạn.", "success");

	} catch (error) {
		console.error("Lỗi khi gọi API signup:", error);
		showMessage("Đã xảy ra lỗi. Vui lòng thử lại.", "error");
	}

});


otpInputs.forEach((input, index) => {
	input.addEventListener('input', (e) => {
		e.target.value = e.target.value.replace(/[^0-9]/g, '').slice(0, 1);

		if (e.target.value.length === 1 && index < otpInputs.length - 1) {
			otpInputs[index + 1].focus();
		}
	});

	input.addEventListener('keydown', (e) => {
		if (e.key === 'Backspace' && e.target.value === '' && index > 0) {
			otpInputs[index - 1].focus();
		}
	});
});



function startCountdown() {
	let timeLeft = 60; // 60 giây
	resendBtn.disabled = true;
	resendBtn.textContent = `Gửi lại OTP (${timeLeft}s)`;
	resendBtn.classList.add('disabled'); // Thêm class để style disable

	countdownTimer = setInterval(() => {
		timeLeft--;
		resendBtn.textContent = `Gửi lại OTP (${timeLeft}s)`;

		if (timeLeft <= 0) {
			clearInterval(countdownTimer);
			resendBtn.disabled = false;
			resendBtn.textContent = 'Gửi lại OTP';
			resendBtn.classList.remove('disabled');
		}
	}, 1000);
}

function startCountdownforgot() {
	let timeLeft = 60; // 60 giây
	resendBtnforgot.disabled = true;
	resendBtnforgot.textContent = `Gửi lại OTP (${timeLeft}s)`;
	resendBtnforgot.classList.add('disabled'); // Thêm class để style disable

	countdownTimer = setInterval(() => {
		timeLeft--;
		resendBtnforgot.textContent = `Gửi lại OTP (${timeLeft}s)`;

		if (timeLeft <= 0) {
			clearInterval(countdownTimer);
			resendBtnforgot.disabled = false;
			resendBtnforgot.textContent = 'Gửi lại OTP';
			resendBtnforgot.classList.remove('disabled');
		}
	}, 1000);
}

function handleResendOTP(button, emailInput) {
	if (button.disabled) return;

	fetch('/api/auth/resend-otp', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
		},
		body: JSON.stringify({ email: emailInput.value }),
	})
		.then(response => {
			if (response.ok) {
				startCountdown();
				showMessage('Đã gửi lại mã OTP mới!', 'success');
			} else {
				showMessage('Lỗi gửi lại OTP. Thử lại sau!', 'error');
			}
		})
		.catch(error => {
			console.error('Lỗi:', error);
			showMessage('Lỗi kết nối. Thử lại sau!', 'error');
		});
}

resendBtn.addEventListener('click', () => handleResendOTP(resendBtn, emailHidden));
resendBtnforgot.addEventListener('click', () => handleResendOTP(resendBtnforgot, emailHidden));

otpForm.addEventListener("submit", async (e) => {
	e.preventDefault();
	const email = document.getElementById("otpEmail").value;

	let otpCode = "";
	otpInputs.forEach(input => otpCode += input.value);

	if (otpCode.length !== 6) {
		showMessage("Vui lòng nhập đầy đủ 6 chữ số OTP.", "error");
		return;
	}

	try {
		const response = await fetch("/api/auth/verify-otp", {
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			},
			body: JSON.stringify({ email: email, otp: otpCode })
		});

		const result = await response.json();

		if (!response.ok) {
			switch (result.errorCode) {
				case "USER_NOT_FOUND":
					showMessage("Email không tồn tại. Vui lòng đăng ký trước.", "error");
					break;
				case "INVALID_OTP":
					showMessage("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.", "error");
					break;
				default:
					showMessage(result.message || "Xác thực OTP thất bại.", "error");
			}
			return;
		}
		
		showMessage(result.message || "Xác thực thành công!", "success");

		alert(result.message);
		signUpForm.reset();
		signInForm.reset();
		signInForm.parentElement.style.display = "";
		signUpForm.parentElement.style.display = "";
		toggle.style.display = "";

		otpContainer.style.display = "none";
		otpForm.reset();

	} catch (error) {
		console.error("Lỗi khi gọi API verifyOTP:", error);
		showMessage("Đã xảy ra lỗi. Vui lòng thử lại.", "error");
	}
});


forgotPasswordLink.addEventListener("click", (e) => {
	e.preventDefault(); // tránh reload trang
	signinFormContainer.style.display = "none"; // ẩn form đăng nhập
	forgotPasswordContainer.style.display = "flex"; // hiện form quên mật khẩu
});


forgotPasswordForm.addEventListener("submit", async (e) => {
	e.preventDefault();

	const email = forgotPasswordForm.email.value;
	if (!email) {
        showMessage("Vui lòng nhập email để khôi phục mật khẩu.", "error");
        return;
    }

	try {
		const response = await fetch("/api/auth/send-otp", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ email })
		});

		const result = await response.json();

		if (!response.ok) {
			showMessage(result.message || "Lỗi gửi OTP. Vui lòng thử lại.", "error");
			return;
		}
		
		showMessage("Đã gửi mã OTP đến email của bạn!", "success");

		forgotOtpEmail.value = email; // Lưu email cho bước verify OTP
		forgotPasswordForm.parentElement.style.display = "none";
		toggle.style.display = "none";
		forgotOtpContainer.style.display = "flex";

		if (typeof startCountdownforgot === "function") {
			startCountdownforgot();
		}

		document.getElementById("otpEmail").value = email;


	} catch (error) {
		console.error(error);
		showMessage("Đã xảy ra lỗi khi gửi OTP. Vui lòng thử lại.", "error");
	}
});

const resetPasswordForm = document.getElementById("resetPasswordForm");
const resetPasswordContainer = document.getElementById("resetPasswordContainer");
otpForgotForm.addEventListener("submit", async (e) => {
	e.preventDefault();
	const email = document.getElementById("otpEmail").value;

	let otpCode = "";
	otpInputs.forEach(input => otpCode += input.value);

	if (otpCode.length !== 6) {
		showMessage("Vui lòng nhập đầy đủ 6 chữ số OTP.", "error");
		return;
	}

	try {
		const response = await fetch("/api/auth/verify-otp", {
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			},
			body: JSON.stringify({ email: email, otp: otpCode })
		});

		const result = await response.json();

		if (!response.ok) {
			switch (result.errorCode) {
				case "USER_NOT_FOUND":
					showMessage("Email không tồn tại. Vui lòng đăng ký trước.", "error");
					break;
				case "INVALID_OTP":
					showMessage("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.", "error");
					break;
				default:
					showMessage(result.message || "Xác thực OTP thất bại.", "error");
			}
			return;
		}

		showMessage(result.message || "Xác thực thành công!", "success");

		forgotOtpContainer.style.display = "none";
		resetPasswordContainer.style.display = "flex";
		toggle.style.display = "";

	} catch (error) {
		console.error("Lỗi khi gọi API verifyOTP:", error);
		showMessage("Đã xảy ra lỗi. Vui lòng thử lại.", "error");
	}
});




resetPasswordForm.addEventListener("submit", async (e) => {
	e.preventDefault();

	const email = document.getElementById("otpEmail").value;
	const newPassword = resetPasswordForm.newPassword.value;
	const confirmPassword = resetPasswordForm.confirmPassword.value;

	if (!newPassword || !confirmPassword) {
		showMessage("Vui lòng nhập đầy đủ thông tin mật khẩu.", "error");
		return;
	}
	
	if (newPassword !== confirmPassword) {
			showMessage("Mật khẩu xác nhận không khớp!", "error");
			return;
		}

	try {
		const response = await fetch("/api/auth/reset", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ email, newPassword })
		});

		const result = await response.json();

		if (!response.ok) {
			showMessage(result.message || "Đặt lại mật khẩu thất bại.", "error");
			return;
		}
		
		showMessage(result.message || "Đặt lại mật khẩu thành công!", "success");

		signUpForm.reset();
		signInForm.reset();
		signInForm.parentElement.style.display = "";
		signUpForm.parentElement.style.display = "";
		toggle.style.display = "";
		resetPasswordContainer.style.display = "none";
		resetPasswordForm.reset();

	} catch (error) {
		console.error(error);
		showMessage("Đã xảy ra lỗi. Vui lòng thử lại.", "error");
	}
});
