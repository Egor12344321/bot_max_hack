# MAX Абитуриент

## Запуск через Docker Compose

Нужен Docker с Compose (например, запущенный Docker Desktop с Linux containers).
Из корня проекта выполните:

```sh
docker compose up --build -d
```

Приложение доступно по адресу http://localhost:5173.
Контейнер собирает фронтенд и раздаёт его через nginx. Прямые переходы и
обновление страниц `/profile`, `/universities` и других маршрутов поддерживаются.
По умолчанию используются моки; отдельный бэкенд для этого режима не нужен.

Compose читает `VITE_API_MODE`, `VITE_API_URL`, `VITE_MAX_MODE` из корневого `.env`.
Образ собирается и без `.env`, с настройками из `.env.example` по умолчанию.
Для другого порта добавьте в `.env`, например, `APP_PORT=3000`.

`VITE_*` встраиваются в клиентскую сборку. После изменения этих настроек или
исходников повторите команду с `--build`. Это запуск готовой сборки без HMR.
Для реального API адрес `VITE_API_URL` должен быть доступен из браузера пользователя;
бэкенд должен разрешать запросы с адреса фронтенда (CORS).

Просмотр логов и остановка:

```sh
docker compose logs -f web
docker compose down
```

## Исходный шаблон: React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```

You can also install [eslint-plugin-react-x](https://npmx.dev/package/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://npmx.dev/package/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```
