# Как зарегистрировать новый endpoint

Этот каталог содержит HTTP-обвязку для Javalin: здесь собираются маршруты, связываются сервисы и настраивается авторизация.

## Актуальный путь в проекте

Сейчас приложение стартует через:

1. [`Main.java`](../../../Main.java)
2. [`InitHandles.java`](InitHandles.java)
3. [`EndpointAuthInitializer.java`](EndpointAuthInitializer.java)

Именно эти классы нужно обновлять, если вы добавляете новый endpoint в текущей архитектуре.

## Общая схема

Для нового endpoint обычно нужны 3 шага:

1. Создать HTTP-обработчик в `com.serbekun.ss.http.handles.v0` или в другом подходящем пакете.
2. Добавить маршрут в `InitHandles.java`.
3. Если endpoint должен быть защищен, добавить его в `EndpointAuthInitializer.java`.

## Шаг 1. Создайте handler

Обычно endpoint не работает напрямую с Javalin-логикой, а вызывает отдельный HTTP-class.

Пример:

```java
public class MyNewHttp {

    public void main(Context ctx) {
        ctx.result("ok");
    }
}
```

Если endpoint использует сервис, передайте его через конструктор, как это сделано в:

- [`ApiV0CipherAesHttp.java`](v0/ApiV0CipherAesHttp.java)
- [`ApiV0CatalogsLinksHttp.java`](v0/ApiV0CatalogsLinksHttp.java)

## Шаг 2. Подключите handler в `InitHandles`

`InitHandles.initHandles(...)` собирает все зависимости и регистрирует `svr.get/post/put/delete(...)`.

Что нужно сделать:

1. Добавить новый handler в параметры `initHandles(...)`, если он использует новый сервис.
2. Создать объект handler внутри метода.
3. Добавить `svr.get(...)`, `svr.post(...)` и т.д. с нужным URL.

Пример:

```java
MyNewHttp myNewHttp = new MyNewHttp(myService);
svr.get("/api/v0/my/new/endpoint", ctx -> myNewHttp.main(ctx));
```

Если endpoint имеет несколько HTTP-методов, добавьте их отдельно:

```java
svr.get("/api/v0/my/new/endpoint", ctx -> myNewHttp.main(ctx));
svr.post("/api/v0/my/new/endpoint", ctx -> myNewHttp.main(ctx));
```

## Шаг 3. Зарегистрируйте endpoint для auth

Если endpoint должен проходить проверку токена, его нужно добавить в `EndpointAuthInitializer.initHandlesAuthSetting(...)`.

Нужно сделать 2 вещи:

1. Создать `Endpoint endpointMyNew = new Endpoint("/api/v0/my/new");`
2. Зарегистрировать его через `endpointRegistrar.register(endpointMyNew, true или false);`
3. Повесить `svr.before(...)` на тот же путь, чтобы endpoint попадал в контекст авторизации.

Пример:

```java
Endpoint endpointMyNew = new Endpoint("/api/v0/my/new");
endpointRegistrar.register(endpointMyNew, true);

svr.before("/api/v0/my/new", ctx -> ctx.attribute("endpoint", endpointMyNew));
```

Если маршрут публичный, регистрируйте его с `false`.

## Как выбрать значение `requiresAuth`

- `true` - endpoint требует токен.
- `false` - endpoint открыт без авторизации.

Сейчас в проекте публичные маршруты выглядят так:

- `/`
- `/static/v0/images/{name}`
- `/static/v0/json`
- `/static/v0/html/{name}`

Приватные маршруты:

- `/api/v0/cipher/aes`
- `/api/v0/cipher/aes/encrypt`
- `/api/v0/cipher/aes/decrypt`
- `/api/v0/catalogs/links`

## Важный момент про matching path

URL в `svr.before(...)` и URL в `svr.get/post/put/delete(...)` должны быть согласованы.

Если endpoint имеет параметры, например `/{uuid}`, обычно:

1. `svr.get("/api/v0/items/{uuid}", ...)`
2. `svr.before("/api/v0/items/{uuid}", ...)`
3. `new Endpoint("/api/v0/items")` или другой базовый ключ для auth, если логика проверки завязана на префикс

Смотрите на текущий пример с:

- [`LinkCatalogRoutes.java`](LinkCatalogRoutes.java)
- [`EndpointAuthInitializer.java`](EndpointAuthInitializer.java)

## Если вы переводите код на новый слой

В репозитории уже есть альтернативная схема:

- [`RouteInitializer.java`](RouteInitializer.java)
- [`AuthInitializer.java`](AuthInitializer.java)
- [`StaticRoutes.java`](StaticRoutes.java)
- [`ApiV0Routes.java`](ApiV0Routes.java)

Если проект начнет использовать их вместо `InitHandles`, инструкция останется той же по смыслу:

1. Создать handler.
2. Подключить его в агрегатор маршрутов.
3. Зарегистрировать endpoint в auth-слое.

## Чеклист

- Handler создан.
- Route добавлен в `InitHandles.java`.
- Endpoint зарегистрирован в `EndpointAuthInitializer.java`.
- Для защищенного endpoint задан `requiresAuth = true`.
- Путь в `before(...)` совпадает с реальным route.
