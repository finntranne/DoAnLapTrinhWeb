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
const forgotPasswordMessage = document.getElementById("forgotPasswordMessage");
const forgotOtpContainer = document.getElementById("forgotOtpContainer");
const forgotOtpEmail = document.getElementById("forgotOtpEmail");
const forgotPasswordLink = document.getElementById("forgotPasswordLink");
const forgotPasswordContainer = document.getElementById("forgotPasswordContainer");

// ============ TOGGLE SIGNUP/SIGNIN FORMS ============
if (registerBtn) {
	registerBtn.addEventListener("click", () => {
		container.classList.add("active");
	});
}

if (loginBtn) {
	loginBtn.addEventListener("click", () => {
		container.classList.remove("active");
	});
}

// ============ SIGNUP FORM HANDLER ============
if (signUpForm) {
	signUpForm.addEventListener("submit", async (e) => {
		e.preventDefault();

		// Clear previous errors
		clearValidationErrors();

		const formData = new FormData(signUpForm);
		const data = {
			fullname: formData.get("fullname"),
			username: formData.get("username"),
			email: formData.get("email"),
			password: formData.get("password"),
			phoneNumber: formData.get("phoneNumber") || null
		};

		try {
			const response = await fetch("/api/auth/signup", {
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				},
				body: JSON.stringify(data)
			});

			const result = await response.json();

			if (!response.ok) {
				// Hiển thị lỗi validation chi tiết
				if (result.errors && typeof result.errors === 'object') {
					displayValidationErrors(result.errors);
					alert(result.message || "Vui lòng kiểm tra lại thông tin");
				} else {
					alert("Đăng ký thất bại: " + (result.message || "Lỗi server"));
				}
				return;
			}

			// Lưu email và hiển thị OTP form
			document.getElementById("otpEmail").value = data.email;

			// Ẩn các form không cần thiết
			document.querySelector('.form-container.sign-up').style.display = "none";
			document.querySelector('.form-container.sign-in').style.display = "none";
			toggle.style.display = "none";

			// Hiển thị OTP container
			otpContainer.style.display = "flex";

			alert("OTP đã được gửi tới email của bạn");
			startCountdown();

		} catch (error) {
			console.error("Lỗi khi gửi API signup:", error);
			alert("Đã xảy ra lỗi. Vui lòng thử lại.");
		}
	});
}

// Function to display validation errors
function displayValidationErrors(errors) {
	for (const [field, message] of Object.entries(errors)) {
		const errorElement = document.getElementById(`${field}-error`);
		if (errorElement) {
			errorElement.textContent = message;
			errorElement.style.display = 'block';
		}
	}
}

// Function to clear all validation errors
function clearValidationErrors() {
	const errorElements = document.querySelectorAll('.error-message');
	errorElements.forEach(element => {
		element.textContent = '';
		element.style.display = 'none';
	});
}

// ============ SIGNIN FORM HANDLER ============
const signinForm = document.getElementById("signinForm");

if (signinForm) {
	signinForm.addEventListener("submit", async (e) => {
		e.preventDefault();

		// Clear previous errors
		clearSigninErrors();

		const formData = new FormData(signinForm);
		const data = {
			usernameOrEmail: formData.get("usernameOrEmail"),
			password: formData.get("password")
		};

		try {
			const response = await fetch("/api/auth/signin", {
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				},
				credentials: 'include', // Quan trọng: để gửi và nhận cookies
				body: JSON.stringify(data)
			});

			const result = await response.json();

			if (!response.ok) {
				// Hiển thị lỗi validation chi tiết
				if (result.errors && typeof result.errors === 'object') {
					displaySigninErrors(result.errors);
					alert(result.message || "Vui lòng kiểm tra lại thông tin");
				} else {
					alert(result.message || "Đăng nhập thất bại");
				}
				return;
			}

			alert(result.message || "Đăng nhập thành công!");

			// Redirect đến URL được trả về từ server
			window.location.href = result.redirectUrl || "/dashboard";

		} catch (error) {
			console.error("Lỗi khi đăng nhập:", error);
			alert("Đã xảy ra lỗi. Vui lòng thử lại.");
		}
	});
}

function displaySigninErrors(errors) {
	for (const [field, message] of Object.entries(errors)) {
		const errorElement = document.getElementById(`signin-${field}-error`);
		if (errorElement) {
			errorElement.textContent = message;
			errorElement.style.display = 'block';
		}
	}
}

function clearSigninErrors() {
	const errorElements = document.querySelectorAll('#signinForm .error-message');
	errorElements.forEach(element => {
		element.textContent = '';
		element.style.display = 'none';
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

		// Ghép 6 input OTP thành 1 chuỗi
		const otpInputsList = document.querySelectorAll('#otpContainer .otp-input');
		let otpCode = "";
		otpInputsList.forEach(input => otpCode += input.value);

		if (otpCode.length !== 6) {
			alert("Vui lòng nhập đầy đủ 6 chữ số OTP.");
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
						alert("Email không tồn tại. Vui lòng đăng ký trước.");
						break;
					case "INVALID_OTP":
						alert("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
						break;
					default:
						alert(result.message || "Xác thực OTP thất bại.");
				}
				return;
			}

			alert(result.message);

			// Reset và hiển thị lại form đăng nhập
			signUpForm.reset();
			document.querySelector('.form-container.sign-in').style.display = "block";
			document.querySelector('.form-container.sign-up').style.display = "block";
			toggle.style.display = "block";
			otpContainer.style.display = "none";
			otpForm.reset();
			container.classList.remove("active");

		} catch (error) {
			console.error("Lỗi khi gửi API verifyOTP:", error);
			alert("Đã xảy ra lỗi. Vui lòng thử lại.");
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

		try {
			const response = await fetch("/api/auth/resend-otp", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email })
			});

			const result = await response.json();

			if (!response.ok) {
				alert(result.message || "Gửi lại OTP thất bại");
				return;
			}

			// Xóa OTP cũ
			const otpInputsList = document.querySelectorAll('#otpContainer .otp-input');
			otpInputsList.forEach(input => input.value = "");

			alert(result.message);
			startCountdown(60);

		} catch (error) {
			console.error("Lỗi khi gửi lại OTP:", error);
			alert("Đã xảy ra lỗi. Vui lòng thử lại.");
		}
	});
}

// ============ FORGOT PASSWORD LINK ============
if (forgotPasswordLink) {
	forgotPasswordLink.addEventListener("click", (e) => {
		e.preventDefault();

		// Ẩn form signin
		document.querySelector('.form-container.sign-in').style.display = "none";

		// Hiển thị form forgot password
		forgotPasswordContainer.style.display = "flex";
	});
}

// ============ FORGOT PASSWORD FORM ============
if (forgotPasswordForm) {
	forgotPasswordForm.addEventListener("submit", async (e) => {
		e.preventDefault();

		// Clear previous errors
		clearForgotPasswordErrors();

		const emailInput = forgotPasswordForm.querySelector('input[name="email"]');
		const email = emailInput.value;

		try {
			const response = await fetch("/api/auth/forgot-password/send-otp", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email })
			});

			const result = await response.json();

			if (!response.ok) {
				// Hiển thị lỗi validation
				if (result.errors && typeof result.errors === 'object') {
					displayForgotPasswordErrors(result.errors);
				} else {
					alert(result.message || "Lỗi gửi OTP.");
				}
				return;
			}

			forgotOtpEmail.value = email;
			forgotPasswordContainer.style.display = "none";
			toggle.style.display = "none";
			forgotOtpContainer.style.display = "flex";

			alert("OTP đã được gửi tới email của bạn");

		} catch (error) {
			console.error(error);
			alert("Đã xảy ra lỗi.");
		}
	});
}

function displayForgotPasswordErrors(errors) {
	for (const [field, message] of Object.entries(errors)) {
		const errorElement = document.getElementById(`forgot-${field}-error`);
		if (errorElement) {
			errorElement.textContent = message;
			errorElement.style.display = 'block';
		}
	}
}

function clearForgotPasswordErrors() {
	const errorElements = document.querySelectorAll('#forgotPasswordForm .error-message');
	errorElements.forEach(element => {
		element.textContent = '';
		element.style.display = 'none';
	});
}

// Back to login button
const backToLoginBtn = document.getElementById("backToLoginBtn");
if (backToLoginBtn) {
	backToLoginBtn.addEventListener("click", () => {
		forgotPasswordContainer.style.display = "none";
		document.querySelector('.form-container.sign-in').style.display = "block";
		toggle.style.display = "block";
		clearForgotPasswordErrors();
	});
}

// ============ FORGOT PASSWORD OTP VERIFICATION ============
const forgotOtpForm = document.getElementById("forgotOtpForm");

if (forgotOtpForm) {
	forgotOtpForm.addEventListener("submit", async (e) => {
		e.preventDefault();

		// Clear previous errors
		clearForgotOtpErrors();

		const forgotOtpInputs = document.querySelectorAll('.forgot-otp');
		let otpCode = "";
		forgotOtpInputs.forEach(input => otpCode += input.value);
		const email = forgotOtpEmail.value;

		if (otpCode.length !== 6) {
			displayForgotOtpError("Vui lòng nhập đủ 6 chữ số OTP.");
			return;
		}

		try {
			const response = await fetch("/api/auth/forgot-password/verify-otp", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email, otp: otpCode })
			});

			const result = await response.json();

			if (!response.ok) {
				if (result.errors && typeof result.errors === 'object') {
					const firstError = Object.values(result.errors)[0];
					displayForgotOtpError(firstError);
				} else {
					displayForgotOtpError(result.message || "OTP không hợp lệ.");
				}
				return;
			}

			// OTP đúng → hiển thị form reset password
			forgotOtpContainer.style.display = "none";
			document.getElementById("resetPasswordContainer").style.display = "flex";

		} catch (error) {
			console.error(error);
			displayForgotOtpError("Đã xảy ra lỗi.");
		}
	});
}

function displayForgotOtpError(message) {
	const errorElement = document.getElementById("forgot-otp-error");
	if (errorElement) {
		errorElement.textContent = message;
		errorElement.style.display = 'block';
	}
}

function clearForgotOtpErrors() {
	const errorElement = document.getElementById("forgot-otp-error");
	if (errorElement) {
		errorElement.textContent = '';
		errorElement.style.display = 'none';
	}
}

// Auto-focus for forgot OTP inputs
const forgotOtpInputs = document.querySelectorAll('.forgot-otp');
if (forgotOtpInputs.length > 0) {
	forgotOtpInputs.forEach((input, index) => {
		input.addEventListener('input', (e) => {
			e.target.value = e.target.value.replace(/[^0-9]/g, '').slice(0, 1);
			if (e.target.value.length === 1 && index < forgotOtpInputs.length - 1) {
				forgotOtpInputs[index + 1].focus();
			}
		});

		input.addEventListener('keydown', (e) => {
			if (e.key === 'Backspace' && e.target.value === '' && index > 0) {
				forgotOtpInputs[index - 1].focus();
			}
		});
	});
}

// ============ RESEND FORGOT PASSWORD OTP ============
const resendForgotOtpBtn = document.getElementById("resendForgotOtpBtn");
if (resendForgotOtpBtn) {
	resendForgotOtpBtn.addEventListener("click", async () => {
		const email = forgotOtpEmail.value;
		if (!email) {
			alert("Không tìm thấy email.");
			return;
		}

		try {
			const response = await fetch("/api/auth/resend-otp", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email })
			});

			if (response.ok) {
				alert("OTP đã được gửi lại");
				startResendForgotOtpCountdown();
			} else {
				alert("Lỗi gửi lại OTP");
			}
		} catch (error) {
			console.error(error);
			alert("Đã xảy ra lỗi.");
		}
	});
}

function startResendForgotOtpCountdown(seconds = 60) {
	if (!resendForgotOtpBtn) return;

	resendForgotOtpBtn.disabled = true;
	let remaining = seconds;
	resendForgotOtpBtn.textContent = `Gửi lại OTP (${remaining}s)`;

	const interval = setInterval(() => {
		remaining--;
		resendForgotOtpBtn.textContent = `Gửi lại OTP (${remaining}s)`;
		if (remaining <= 0) {
			clearInterval(interval);
			resendForgotOtpBtn.disabled = false;
			resendForgotOtpBtn.textContent = "Gửi lại OTP";
		}
	}, 1000);
}

// ============ RESET PASSWORD FORM ============
if (resetPasswordForm) {
	resetPasswordForm.addEventListener("submit", async (e) => {
		e.preventDefault();

		// Clear previous errors
		clearResetPasswordErrors();

		const email = forgotOtpEmail.value;
		const newPassword = resetPasswordForm.querySelector('input[name="newPassword"]').value;
		const confirmPassword = resetPasswordForm.querySelector('input[name="confirmPassword"]').value;

		// Client-side validation
		if (newPassword !== confirmPassword) {
			displayResetPasswordError("confirmPassword", "Mật khẩu xác nhận không khớp!");
			return;
		}

		if (newPassword.length < 6) {
			displayResetPasswordError("newPassword", "Mật khẩu phải có ít nhất 6 ký tự!");
			return;
		}

		try {
			const response = await fetch("/api/auth/forgot-password/reset", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email, newPassword, confirmPassword })
			});

			const result = await response.json();

			if (!response.ok) {
				if (result.errors && typeof result.errors === 'object') {
					displayResetPasswordErrors(result.errors);
				} else {
					alert(result.message || "Đặt lại mật khẩu thất bại.");
				}
				return;
			}

			alert(result.message || "Mật khẩu đã được đặt lại thành công!");

			// Reset và hiển thị lại form đăng nhập
			resetPasswordForm.reset();
			document.querySelector('.form-container.sign-in').style.display = "block";
			document.querySelector('.form-container.sign-up').style.display = "block";
			toggle.style.display = "block";
			resetPasswordContainer.style.display = "none";
			forgotPasswordContainer.style.display = "none";
			container.classList.remove("active");

		} catch (error) {
			console.error(error);
			alert("Đã xảy ra lỗi.");
		}
	});
}

function displayResetPasswordErrors(errors) {
	for (const [field, message] of Object.entries(errors)) {
		displayResetPasswordError(field, message);
	}
}

function displayResetPasswordError(field, message) {
	const errorElement = document.getElementById(`reset-${field}-error`);
	if (errorElement) {
		errorElement.textContent = message;
		errorElement.style.display = 'block';
	}
}

function clearResetPasswordErrors() {
	const errorElements = document.querySelectorAll('#resetPasswordForm .error-message');
	errorElements.forEach(element => {
		element.textContent = '';
		element.style.display = 'none';
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
