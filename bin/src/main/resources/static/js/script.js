// ============ DOM ELEMENTS ============
const container = document.getElementById("container");
const registerBtn = document.getElementById("register");
const loginBtn = document.getElementById("login");
const signUpForm = document.getElementById("signupForm");
const toggle = document.getElementById("toggle");
const otpContainer = document.getElementById("otpContainer");
const otpInputs = document.querySelectorAll('.otp-input');
const resendBtn = document.getElementById('resend-otp');
const otpForm = document.getElementById("otpForm");
const otpEmail = document.getElementById("otpEmail");
const forgotPasswordForm = document.getElementById("forgotPasswordForm");
const forgotPasswordContainer = document.getElementById("forgotPasswordContainer");
const forgotPasswordLink = document.getElementById("forgotPasswordLink");
const backToLoginBtn = document.getElementById("backToLoginBtn");

// ============ TOGGLE SIGNUP/SIGNIN FORMS ============
if (registerBtn) {
	registerBtn.addEventListener("click", () => {
		// Kiểm tra nếu đang ở trang forgot password
		if (forgotPasswordContainer && forgotPasswordContainer.style.display === "flex") {
			// Ẩn forgot password
			forgotPasswordContainer.style.display = "none";

			// Hiển thị lại các form và toggle
			document.querySelector('.form-container.sign-in').style.display = "block";
			document.querySelector('.form-container.sign-up').style.display = "block";
			document.querySelector('.toggle-panel.toggle-left').style.display = "flex";
			toggle.style.display = "block";

			// Chuyển sang form đăng ký với animation
			setTimeout(() => {
				container.classList.add("active");
			}, 50);
		} else {
			// Chuyển đổi bình thường
			container.classList.add("active");
		}
	});
}

if (loginBtn) {
	loginBtn.addEventListener("click", () => {
		// Chuyển về form đăng nhập với animation
		container.classList.remove("active");
	});
}

// ============ SIGNUP FORM HANDLER ============
if (signUpForm) {
	signUpForm.addEventListener("submit", async (e) => {
		e.preventDefault();
		clearValidationErrors();

		const formData = new FormData(signUpForm);
		const data = {
			fullname: formData.get("fullname"),
			username: formData.get("username"),
			email: formData.get("email"),
			password: formData.get("password"),
			phoneNumber: formData.get("phoneNumber") || null
		};

		// Lấy nút submit
		const submitButton = signUpForm.querySelector('button[type="submit"]');
		const originalText = submitButton.textContent;

		// Disable button và hiển thị trạng thái
		submitButton.disabled = true;
		submitButton.textContent = "Đang gửi OTP...";

		try {
			const response = await fetch("/api/auth/signup", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(data)
			});

			const result = await response.json();

			if (!response.ok) {
				if (result.errors && typeof result.errors === 'object') {
					displayValidationErrors(result.errors);
					alert(result.message || "Vui lòng kiểm tra lại thông tin");
				} else {
					alert("Đăng ký thất bại: " + (result.message || "Lỗi server"));
				}

				// Enable lại button
				submitButton.disabled = false;
				submitButton.textContent = originalText;
				return;
			}

			// Kiểm tra nếu có redirectToOtp trong response
			if (result.redirectToOtp && result.email) {
				document.getElementById("otpEmail").value = result.email;
				document.querySelector('.form-container.sign-up').style.display = "none";
				document.querySelector('.form-container.sign-in').style.display = "none";
				toggle.style.display = "none";
				otpContainer.style.display = "flex";

				alert(result.message || "OTP đã được gửi tới email của bạn");

				// Start countdown với thời gian còn lại
				if (result.otpTimeRemaining) {
					// Nếu OTP còn hạn, hiển thị countdown với thời gian còn lại
					const remainingSeconds = Math.max(0, Math.floor(result.otpTimeRemaining));
					if (remainingSeconds > 0) {
						startCountdown(remainingSeconds);
					}
				} else {
					// Mặc định 60 giây nếu không có thông tin
					startCountdown(60);
				}
			}

			// Reset button về trạng thái ban đầu
			submitButton.disabled = false;
			submitButton.textContent = originalText;

		} catch (error) {
			console.error("Lỗi khi gửi API signup:", error);
			alert("Đã xảy ra lỗi. Vui lòng thử lại.");

			// Enable lại button
			submitButton.disabled = false;
			submitButton.textContent = originalText;
		}
	});
}

function displayValidationErrors(errors) {
	for (const [field, message] of Object.entries(errors)) {
		const errorElement = document.getElementById(`${field}-error`);
		if (errorElement) {
			errorElement.textContent = message;
			errorElement.classList.add('show'); // Chỉ dùng class, không dùng style.display
		}
	}
}

function clearValidationErrors() {
	const errorElements = document.querySelectorAll('.error-message');
	errorElements.forEach(element => {
		element.textContent = '';
		element.classList.remove('show'); // Chỉ dùng class, không dùng style.display
	});
}

// ============ SIGNIN FORM HANDLER ============
const signinForm = document.getElementById("signinForm");

if (signinForm) {
	signinForm.addEventListener("submit", async (e) => {
		e.preventDefault();
		clearSigninErrors();

		const formData = new FormData(signinForm);
		const data = {
			usernameOrEmail: formData.get("usernameOrEmail"),
			password: formData.get("password")
		};

		// Lấy nút submit
		const submitButton = signinForm.querySelector('button[type="submit"]');
		const originalText = submitButton.textContent;

		// Disable button và hiển thị trạng thái
		submitButton.disabled = true;
		submitButton.textContent = "Đang đăng nhập...";

		try {
			const response = await fetch("/api/auth/signin", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				credentials: 'include',
				body: JSON.stringify(data)
			});

			const result = await response.json();

			if (!response.ok) {
				if (result.errors && typeof result.errors === 'object') {
					displaySigninErrors(result.errors);
					alert(result.message || "Vui lòng kiểm tra lại thông tin");
				} else {
					alert(result.message || "Đăng nhập thất bại");
				}

				// Enable lại button
				submitButton.disabled = false;
				submitButton.textContent = originalText;
				return;
			}

			alert(result.message || "Đăng nhập thành công!");
			window.location.href = result.redirectUrl || "/dashboard";

		} catch (error) {
			console.error("Lỗi khi đăng nhập:", error);
			alert("Đã xảy ra lỗi. Vui lòng thử lại.");

			// Enable lại button
			submitButton.disabled = false;
			submitButton.textContent = originalText;
		}
	});
}

function displaySigninErrors(errors) {
	for (const [field, message] of Object.entries(errors)) {
		const errorElement = document.getElementById(`signin-${field}-error`);
		if (errorElement) {
			errorElement.textContent = message;
			errorElement.classList.add('show'); // Chỉ dùng class
		}
	}
}

function clearSigninErrors() {
	const errorElements = document.querySelectorAll('#signinForm .error-message');
	errorElements.forEach(element => {
		element.textContent = '';
		element.classList.remove('show'); // Chỉ dùng class
	});
}

// ============ OTP INPUT AUTO-FOCUS ============
if (otpInputs.length > 0) {
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
}

// ============ OTP VERIFICATION ============
if (otpForm) {
	otpForm.addEventListener("submit", async (e) => {
		e.preventDefault();

		const email = document.getElementById("otpEmail").value;
		const otpInputsList = document.querySelectorAll('#otpContainer .otp-input');
		let otpCode = "";
		otpInputsList.forEach(input => otpCode += input.value);

		if (otpCode.length !== 6) {
			alert("Vui lòng nhập đầy đủ 6 chữ số OTP.");
			return;
		}

		// Lấy nút submit
		const submitButton = otpForm.querySelector('button[type="submit"]');
		const originalText = submitButton.textContent;

		// Disable button và hiển thị trạng thái
		submitButton.disabled = true;
		submitButton.textContent = "Đang xác thực...";

		try {
			const response = await fetch("/api/auth/verify-otp", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email: email, otp: otpCode })
			});

			const result = await response.json();

			if (!response.ok) {
				switch (result.errorCode) {
					case "USER_NOT_FOUND":
						alert("Email không tồn tại. Vui lòng đăng ký trước.");
						break;
					case "INVALID_OTP":
						alert("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
						break;
					default:
						alert(result.message || "Xác thực OTP thất bại.");
				}

				// Enable lại button
				submitButton.disabled = false;
				submitButton.textContent = originalText;
				return;
			}

			alert(result.message);

			// Reload lại trang để reset về trạng thái đăng nhập ban đầu
			window.location.href = "/login";

		} catch (error) {
			console.error("Lỗi khi gửi API verifyOTP:", error);
			alert("Đã xảy ra lỗi. Vui lòng thử lại.");

			// Enable lại button
			submitButton.disabled = false;
			submitButton.textContent = originalText;
		}
	});
}

// ============ RESEND OTP ============
function startCountdown(seconds = 60) {
	if (!resendBtn) return;

	resendBtn.disabled = true;
	let remaining = seconds;
	resendBtn.textContent = `Gửi lại OTP (${remaining}s)`;

	const interval = setInterval(() => {
		remaining--;
		resendBtn.textContent = `Gửi lại OTP (${remaining}s)`;
		if (remaining <= 0) {
			clearInterval(interval);
			resendBtn.disabled = false;
			resendBtn.textContent = "Gửi lại OTP";
		}
	}, 1000);
}

if (resendBtn) {
	resendBtn.addEventListener("click", async () => {
		const email = document.getElementById("otpEmail").value;
		if (!email) {
			alert("Không tìm thấy email. Vui lòng thử lại từ đầu.");
			return;
		}

		// Lưu text gốc và disable button
		const originalText = resendBtn.textContent;
		resendBtn.disabled = true;
		resendBtn.textContent = "Đang gửi OTP...";

		try {
			const response = await fetch("/api/auth/resend-otp", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email })
			});

			const result = await response.json();

			if (!response.ok) {
				alert(result.message || "Gửi lại OTP thất bại");
				resendBtn.disabled = false;
				resendBtn.textContent = originalText;
				return;
			}

			const otpInputsList = document.querySelectorAll('#otpContainer .otp-input');
			otpInputsList.forEach(input => input.value = "");

			alert(result.message);
			startCountdown(60);

		} catch (error) {
			console.error("Lỗi khi gửi lại OTP:", error);
			alert("Đã xảy ra lỗi. Vui lòng thử lại.");
			resendBtn.disabled = false;
			resendBtn.textContent = originalText;
		}
	});
}

// ============ FORGOT PASSWORD LINK ============
if (forgotPasswordLink) {
	forgotPasswordLink.addEventListener("click", (e) => {
		e.preventDefault();

		// Ẩn form signin
		document.querySelector('.form-container.sign-in').style.display = "none";
		document.querySelector('.form-container.sign-up').style.display = "none";

		// Ẩn toggle bên trái, giữ lại panel bên phải
		document.querySelector('.toggle-panel.toggle-left').style.display = "none";

		// Hiển thị form forgot password
		forgotPasswordContainer.style.display = "flex";
	});
}

// ============ BACK TO LOGIN ============
if (backToLoginBtn) {
	backToLoginBtn.addEventListener("click", () => {
		// Ẩn forgot password
		forgotPasswordContainer.style.display = "none";

		// Hiển thị lại các form và toggle
		document.querySelector('.form-container.sign-in').style.display = "block";
		document.querySelector('.form-container.sign-up').style.display = "block";
		document.querySelector('.toggle-panel.toggle-left').style.display = "flex";
		toggle.style.display = "block";

		// Đảm bảo về form đăng nhập
		container.classList.remove("active");

		// Clear errors
		clearForgotPasswordErrors();
	});
}

// ============ FORGOT PASSWORD FORM ============
if (forgotPasswordForm) {
	forgotPasswordForm.addEventListener("submit", async (e) => {
		e.preventDefault();
		clearForgotPasswordErrors();

		const emailInput = forgotPasswordForm.querySelector('input[name="email"]');
		const email = emailInput.value;
		const submitButton = forgotPasswordForm.querySelector('button[type="submit"]');

		submitButton.disabled = true;
		submitButton.textContent = "Đang gửi...";

		try {
			const response = await fetch("/api/auth/forgot-password", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email })
			});

			const result = await response.json();

			if (!response.ok) {
				// ===== XỬ LÝ TÀI KHOẢN CHƯA XÁC THỰC =====
				if (result.errorCode === "ACCOUNT_NOT_VERIFIED") {
					// Tài khoản chưa xác thực, OTP còn hạn
					alert(result.message);

					// Chuyển đến trang OTP
					if (result.needVerification && result.email) {
						// Ẩn forgot password
						forgotPasswordContainer.style.display = "none";
						document.querySelector('.form-container.sign-up').style.display = "none";
						document.querySelector('.form-container.sign-in').style.display = "none";

						const toggleRight = document.querySelector('.toggle-panel.toggle-right');
						if (toggleRight) toggleRight.style.display = "none";

						toggle.style.display = "none";
						

						// Hiển thị form OTP
						document.getElementById("otpEmail").value = result.email;
						otpContainer.style.display = "flex";

						// Hiển thị countdown với thời gian còn lại
						if (result.otpTimeRemaining) {
							const remainingSeconds = Math.max(0, Math.floor(result.otpTimeRemaining));
							if (remainingSeconds > 0) {
								startCountdown(remainingSeconds);
							}
						}
					}

					submitButton.disabled = false;
					submitButton.textContent = "Gửi Link";
					return;
				}

				if (result.errorCode === "ACCOUNT_NOT_VERIFIED_EXPIRED") {
					// Tài khoản chưa xác thực, OTP hết hạn - yêu cầu đăng ký lại
					alert(result.message);

					// Chuyển về trang đăng ký
					forgotPasswordContainer.style.display = "none";
					document.querySelector('.form-container.sign-in').style.display = "block";
					document.querySelector('.form-container.sign-up').style.display = "block";
					document.querySelector('.toggle-panel.toggle-left').style.display = "flex";
					toggle.style.display = "block";

					// Chuyển sang form đăng ký với animation
					setTimeout(() => {
						container.classList.add("active");
					}, 100);

					submitButton.disabled = false;
					submitButton.textContent = "Gửi Link";
					return;
				}

				if (result.errorCode === "ACCOUNT_SUSPENDED") {
					// Tài khoản bị khóa
					alert(result.message);
					submitButton.disabled = false;
					submitButton.textContent = "Gửi Link";
					return;
				}

				// ===== XỬ LÝ CÁC LỖI KHÁC =====
				if (result.errors) {
					displayForgotPasswordErrors(result.errors);
				} else {
					alert(result.message || "Lỗi gửi yêu cầu.");
				}
				submitButton.disabled = false;
				submitButton.textContent = "Gửi Link";
				return;
			}

			// ===== THÀNH CÔNG =====
			alert(result.message || "Link đặt lại mật khẩu đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư (kể cả thư spam).");

			// Reload lại trang để reset về trạng thái ban đầu
			window.location.href = "/login";

		} catch (error) {
			console.error("Lỗi:", error);
			alert("Đã xảy ra lỗi kết nối. Vui lòng thử lại.");
			submitButton.disabled = false;
			submitButton.textContent = "Gửi Link";
		}
	});
}

function displayForgotPasswordErrors(errors) {
	for (const [field, message] of Object.entries(errors)) {
		const errorElement = document.getElementById(`forgot-${field}-error`);
		if (errorElement) {
			errorElement.textContent = message;
			errorElement.classList.add('show'); // Chỉ dùng class
		}
	}
}

function clearForgotPasswordErrors() {
	const errorElements = document.querySelectorAll('#forgotPasswordForm .error-message');
	errorElements.forEach(element => {
		element.textContent = '';
		element.classList.remove('show'); // Chỉ dùng class
	});
}

// ============ LOGOUT HANDLER ============
function logout() {
	if (!confirm("Bạn có chắc chắn muốn đăng xuất?")) return;

	fetch("/api/auth/logout", {
		method: "POST",
		credentials: "include"
	})
		.then(res => res.json())
		.then(result => {
			alert(result.message || "Đăng xuất thành công!");
			window.location.href = "/login";
		})
		.catch(error => {
			console.error("Lỗi khi đăng xuất:", error);
			alert("Đã xảy ra lỗi khi đăng xuất.");
			window.location.href = "/login";
		});
}

document.addEventListener('DOMContentLoaded', function() {
	const logoutButtons = document.querySelectorAll('.logout-btn, #logoutBtn, [data-logout]');
	logoutButtons.forEach(btn => btn.addEventListener('click', e => {
		e.preventDefault();
		logout();
	}));
});