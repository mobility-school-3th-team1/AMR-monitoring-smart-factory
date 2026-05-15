<template>
  <form class="login-form">
    <h2 class="login-form__title">로그인</h2>
    <p class="login-form__description">프론트엔드 프로젝트 골격 정리용 초기 로그인 화면입니다.</p>

    <label class="login-field">
      <span>아이디</span>
      <input type="text" placeholder="admin@factory.local" />
    </label>

    <label class="login-field">
      <span>비밀번호</span>
      <input type="password" placeholder="••••••••" />
    </label>

    <button class="login-button" type="button">로그인</button>
  </form>
</template>

<style scoped>
.login-form {
  display: grid;
  gap: 16px;
}

.login-form__title {
  margin: 0;
  font-size: 1.6rem;
  letter-spacing: -0.04em;
}

.login-form__description {
  margin: 0 0 8px;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.login-field {
  display: grid;
  gap: 8px;
}

.login-field span {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.login-field input {
  min-height: 48px;
  padding: 0 16px;
  border-radius: 14px;
  border: 1px solid rgba(217, 228, 240, 0.95);
  background: #fff;
}

.login-button {
  min-height: 50px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-strong));
  color: #fff;
  font-weight: 800;
}
</style>