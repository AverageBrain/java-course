#Java-Advanced-Course 2023
Решения для [Java Advanced course](https://www.kgeorgiy.info/courses/java-advanced/)

## Walk
* Разработать классы `Walk`, `RecursiveWalk`
* Формат запуска: `java Walk` <входной файл> <выходной файл>. 
* Выходной файл содержит хеш файла(SHA-256) и путь к файлу, если произошла ошибка при чтении, то в качестве хэша выводятся все нули

## ArraySet
* Класс реализует интерфейс [NavigableSet](https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/util/NavigableSet.html)

## StudentsBD
* Домашнее задание на использование [Stream API](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html)
* Класс реализующий интерфейс `AdvancedQuery`

## Implementor
* Класс, генерирующий реализации классов и интерфейсов
* При запуске с аргументом -jar, должен генерировать .jar-файл с реализацией

## Concurrent
* Итеративный параллелизм, реализовать `IterativeParallelism`, который обрабатывает списки в несколько поток и реализует метода
	* minimum(threads, list, comparator) — первый минимум;
	* maximum(threads, list, comparator) — первый максимум;
	* all(threads, list, predicate) — проверка, что все элементы списка, удовлетворяют предикату;
	* any(threads, list, predicate) — проверка, что существует элемент списка, удовлетворяющий предикату.
	* count(threads, list, predicate) — подсчёт числа элементов списка, удовлетворяющих предикату.
	* filter(threads, list, predicate) — вернуть список, содержащий элементы удовлетворяющие предикату;
	* map(threads, list, function) — вернуть список, содержащий результаты применения функции;
	* join(threads, list) — конкатенация строковых представлений элементов списка.
	* reduce(threads, list, monoid) — сжатие элементов, используя моноид
	* mapReduce(threads, list, function, monoid) - применить функцию и сжать, исплдбзуя моноид
* Параллельный запуск, напишите класс `ParallelMapperImpl`, реализующий интерфейс `ParallelMapper`
	* Метод `map` должен параллельно вычислять функцию `f` на каждом из указанных аргументов (`args`).
	* Метод `close` должен останавливать все рабочие потоки.
	* Конструктор `ParallelMapperImpl(int threads)` создает threads рабочих потоков, которые могут быть использованы для распараллеливания.
	* Задания на исполнение должны накапливаться в очереди и обрабатываться в порядке поступления.

## Web-Crawler
* Написать потокобезопасный класс, который рекурсивно обходит сайты
* Реализует интерфейс `Crawler`
* Для загразки используется [`Downloader`](https://github.com/AverageBrain/java-course/blob/main/test/modules/info.kgeorgiy.java.advanced.crawler/info/kgeorgiy/java/advanced/crawler/Downloader.java)

## HelloUDP и HelloNonBlockingUDP
* Реализовать сервер и клиент, взаимодействующие по UDP
	* Класс `HelloUDPClient` отправляет запросы на сервер, принимает результаты и выводит их на консоль.
		* Запросы одновременно отсылаются в указанном числе потоков. Каждый поток ожидает обработки своего запроса и выводит сам запрос и результат его обработки на консоль. Если запрос не был обработан, он посылается заново.
		* Запросы должны формироваться по схеме `<префикс запросов><номер потока>_<номер запроса в потоке>`
	* Класс `HelloUDPServer` принимает задания, отсылаемые классом `HelloUDPClient` и отвечает на них.
		* Ответ - `Hello, <текст запроса>`
* Реализуйте клиент и сервер, взаимодействующие по `UDP`, используя только неблокирующий ввод-вывод.
