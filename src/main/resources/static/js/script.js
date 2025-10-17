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
const otpForm = document.getElementById("otpForm");
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

document.addEventListener('DOMContentLoaded', () => {
    const signinForm = document.getElementById('signinForm');
    const usernameOrEmailInput = document.querySelector('input[name="usernameOrEmail"]');
    const passwordInput = document.querySelector('input[name="passwords"]');

    // Theo dõi giá trị password khi nhập
    passwordInput.addEventListener('input', (e) => {
        console.log('Password updated:', e.target.value);
    });

    signinForm.addEventListener('submit', async (e) => {
        e.preventDefault();   

        console.log('Username/Email Input:', usernameOrEmailInput);
        console.log('Password Input:', passwordInput);
        console.log('Username/Email Value:', usernameOrEmailInput ? usernameOrEmailInput.value : 'Not found');
        console.log('Password Value at submit:', passwordInput ? passwordInput.value : 'Not found');

        const usernameOrEmail = usernameOrEmailInput ? usernameOrEmailInput.value : '';
        const password = passwordInput ? passwordInput.value : '';

        if (!usernameOrEmail || !password) {
            alert("Vui lòng nhập đầy đủ email/tên đăng nhập và mật khẩu!");
            return;
        }

        const data = {
            usernameOrEmail: usernameOrEmail,
            password: password
        };

        try {
            const response = await fetch('/api/auth/signin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            const responseText = await response.text();
            console.log('Status:', response.status);
            console.log('Response:', responseText);

            if (!response.ok) {
                try {
                    const errorData = JSON.parse(responseText);
                    alert("Đăng nhập thất bại: " + (errorData.message || "Lỗi server"));
                } catch (e) {
                    alert("Đăng nhập thất bại: Phản hồi không phải JSON - " + responseText.substring(0, 100));
                }
                return;
            }

            try {
                const result = JSON.parse(responseText);
                alert(result.message || "Đăng nhập thành công!");
                document.querySelector('.form-container.sign-in').style.display = 'none';
                window.location.href = '/';
            } catch (e) {
                alert("Lỗi khi xử lý phản hồi: " + responseText.substring(0, 100));
            }

        } catch (error) {
            console.error('Lỗi:', error);
            alert('Đã xảy ra lỗi khi gọi API.');
        }
    });
});

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
        const response = await fetch("/api/auth/signup", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorData = await response.json();
            alert("Đăng ký thất bại: " + (errorData.message || "Lỗi server"));
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
		
		
    } catch (error) {
        console.error("Lỗi khi gọi API signup:", error);
        alert("Đã xảy ra lỗi. Vui lòng thử lại.");
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


  resendBtn.addEventListener('click', function () {
    if (!resendBtn.disabled) {
      
      fetch('/api/auth/resend-otp', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: emailHidden.value }),
      })
      .then(response => {
        if (response.ok) {
       
          startCountdown();

          alert('Đã gửi lại mã OTP mới!');
        } else {
          alert('Lỗi gửi lại OTP. Thử lại sau!');
        }
      })
      .catch(error => {
        console.error('Lỗi:', error);
        alert('Lỗi kết nối. Thử lại sau!');
      });
    }
  });
  
  
  otpForm.addEventListener("submit", async (e) => {
	e.preventDefault();

    
	const email = document.getElementById("otpEmail").value;


    // Ghép 6 input OTP thành 1 chuỗi
    let otpCode = "";
    otpInputs.forEach(input => otpCode += input.value);

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
            // Xử lý lỗi theo errorCode
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
		signUpForm.reset();
		signInForm.reset();
		signInForm.parentElement.style.display = "";
		signUpForm.parentElement.style.display = "";
		toggle.style.display = "";

		// Ẩn container OTP
		otpContainer.style.display = "none";
		otpForm.reset();
        

    } catch (error) {
        console.error("Lỗi khi gọi API verifyOTP:", error);
        alert("Đã xảy ra lỗi. Vui lòng thử lại.");
    }
  });
  
  
  
  function startResendCountdown(seconds = 60) {
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

  // Khi nhấn resend OTP
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

          // Xóa OTP cũ trên form
          otpInputs.forEach(input => input.value = "");

          alert(result.message);
          startResendCountdown(60); // bắt đầu countdown 60s

      } catch (error) {
          console.error("Lỗi khi gửi lại OTP:", error);
          alert("Đã xảy ra lỗi. Vui lòng thử lại.");
      }
  });
  
  forgotPasswordLink.addEventListener("click", (e) => {
        e.preventDefault(); // tránh reload trang
        signinFormContainer.style.display = "none"; // ẩn form đăng nhập
        forgotPasswordContainer.style.display = "flex"; // hiện form quên mật khẩu
    });
    
  
  forgotPasswordForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      forgotPasswordMessage.textContent = "";

      const email = forgotPasswordForm.email.value;

      try {
          const response = await fetch("/api/auth/forgot-password/send-otp", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ email })
          });

          const result = await response.json();

          if (!response.ok) {
              forgotPasswordMessage.textContent = result.message || "Lỗi gửi OTP.";
              return;
          }

          forgotOtpEmail.value = email; // Lưu email cho bước verify OTP
          forgotPasswordForm.parentElement.style.display = "none";
		  toggle.style.display = "none";
          forgotOtpContainer.style.display = "flex";
        

      } catch (error) {
          console.error(error);
          forgotPasswordMessage.textContent = "Đã xảy ra lỗi.";
      }
  });

  
  const verifyForgotOtpBtn = document.getElementById("verifyForgotOtpBtn");
  const forgotOtpMessage = document.getElementById("forgotOtpMessage");

  verifyForgotOtpBtn.addEventListener("click", async () => {
      forgotOtpMessage.textContent = "";

      let otpCode = "";
      otpInputs.forEach(input => otpCode += input.value);
      const email = forgotOtpEmail.value;

      if (otpCode.length !== 6) {
          forgotOtpMessage.textContent = "Vui lòng nhập đủ 6 chữ số OTP.";
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
              forgotOtpMessage.textContent = result.message || "OTP không hợp lệ.";
              return;
          }

          // OTP đúng → hiển thị form reset password
          forgotOtpContainer.style.display = "none";
          document.getElementById("resetPasswordContainer").style.display = "flex";
		  toggle.style.display = "";
      } catch (error) {
          console.error(error);
          forgotOtpMessage.textContent = "Đã xảy ra lỗi.";
      }
  });

  // 3. Reset mật khẩu
  const resetPasswordForm = document.getElementById("resetPasswordForm");
  const resetPasswordMessage = document.getElementById("resetPasswordMessage");
  const resetPasswordContainer = document.getElementById("resetPasswordContainer");

  resetPasswordForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      resetPasswordMessage.textContent = "";

      const email = forgotOtpEmail.value;
      const newPassword = resetPasswordForm.newPassword.value;
      const confirmPassword = resetPasswordForm.confirmPassword.value;

      if (newPassword !== confirmPassword) {
          resetPasswordMessage.textContent = "Mật khẩu xác nhận không khớp!";
          return;
      }

      try {
          const response = await fetch("/api/auth/forgot-password/reset", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ email, newPassword })
          });

          const result = await response.json();

          if (!response.ok) {
              resetPasswordMessage.textContent = result.message || "Đặt lại mật khẩu thất bại.";
              return;
          }


		  signUpForm.reset();
  		  signInForm.reset();
  		  signInForm.parentElement.style.display = "";
  		  signUpForm.parentElement.style.display = "";
  		  toggle.style.display = "";
		  resetPasswordContainer.style.display = "none";
		  resetPasswordForm.reset();
		  
      } catch (error) {
          console.error(error);
          resetPasswordMessage.textContent = "Đã xảy ra lỗi.";
      }
  });
  
  document.addEventListener("DOMContentLoaded", function() {
      const topNav = document.getElementById("top-nav");
      const mainNav = document.getElementById("main-nav");

      if (!topNav || !mainNav) return;

      const triggerPoint = topNav.offsetHeight; // khi cuộn qua chiều cao của thanh đầu

      window.addEventListener("scroll", function() {
          if (window.scrollY > triggerPoint) {
              mainNav.classList.add("sticky");
          } else {
              mainNav.classList.remove("sticky");
          }
      });
  });


  