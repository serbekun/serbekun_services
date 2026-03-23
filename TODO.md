1. Изменить класs serbekun_service/src/main/java/com/serbekun/service/resource/ResourcesService.java чтоб он в начале проверал существует ли файл в файловой сисьтеме а потом проверал то что есть в jar чтоб могли быть динамический ресурсы и ABI не поменялься

2. сделать src/main/java/com/serbekun/http/handles/v0/V0Links.java