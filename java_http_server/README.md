# java_http_server

Serwer HTTP w Javie oparty na gniazdach TCP. Powstał na potrzeby portalu aukcyjnego. Nasłuchuje na skonfigurowanym porcie, odczytuje żądanie HTTP, rozdziela ścieżkę, parametry i nagłówki, a następnie przekazuje dane do klasy obsługującej daną trasę.

Po uruchomieniu serwer udostępnia stronę powitalną pod `GET /`. Można go używać bez bazy albo włączyć moduł PostgreSQL z pulą połączeń HikariCP. W portalu moduł ten dostarczał na stronę główną dane o najbliższej nadchodzącej aukcji.

## Uruchomienie bez bazy

Potrzebujesz JDK z poleceniami `javac` i `java`. Projekt działa z JDK 11. Biblioteki są dołączone w katalogu [jar](jar/). Polecenia wykonuj z katalogu zawierającego `mainApp.java`.

1. **Ustaw adres nasłuchiwania.** W konstruktorze w [httpServer.java](httpServer/httpServer.java) adres IP jest wpisany bezpośrednio przy tworzeniu `InetSocketAddress`. Do testów na własnym komputerze ustaw adres pętli zwrotnej `127.0.0.1`.

   ```java
   InetSocketAddress sockAddr = new InetSocketAddress("127.0.0.1", port);
   ```

2. **Przygotuj plik `.dataEnv`** według przykładu poniżej. W trybie podstawowym możesz pozostawić przykładowe dane bazy. Port HTTP musi być wolny.

3. **Skompiluj i uruchom serwer.** Poniższe polecenia działają w PowerShellu i CMD.

   ```powershell
   javac -cp ./;jar/javax.json-1.1.4.jar;jar/postgresql-42.7.2.jar;jar/HikariCP-7.0.2.jar;jar/slf4j-api-1.7.2.jar;jar/slf4j-simple-1.7.2.jar mainApp.java
   
   java -cp ./;jar/javax.json-1.1.4.jar;jar/postgresql-42.7.2.jar;jar/HikariCP-7.0.2.jar;jar/slf4j-api-1.7.2.jar;jar/slf4j-simple-1.7.2.jar mainApp .dataEnv,
   ```

   W Linuksie użyj dwukropka jako separatora ścieżek.

   ```bash
   javac -cp ./;jar/javax.json-1.1.4.jar;jar/postgresql-42.7.2.jar;jar/HikariCP-7.0.2.jar;jar/slf4j-api-1.7.2.jar;jar/slf4j-simple-1.7.2.jar mainApp.java

   java -cp ./;jar/javax.json-1.1.4.jar;jar/postgresql-42.7.2.jar;jar/HikariCP-7.0.2.jar;jar/slf4j-api-1.7.2.jar;jar/slf4j-simple-1.7.2.jar mainApp .dataEnv
   ```

   Projekt zawiera też [konfigurację VS Code](.vscode/launch.json) uruchamiającą `mainApp` z argumentem `.dataEnv` oraz [skrypty npm dla Windows](package.json) — `npm run compile` i `npm start`.

4. **Otwórz** `http://127.0.0.1:56571/`. Strona wyświetli tekst **Welcome to Java Http Server!**. Aby sprawdzić również kod odpowiedzi, wykonaj polecenie w drugim terminalu Windows.

   ```powershell
   curl.exe -i http://127.0.0.1:56571/
   ```

   Pierwszy wiersz odpowiedzi to `HTTP/1.1 200 OK`. Dla nieznanej ścieżki, np. `/brak`, serwer zwraca `404`. Aby zakończyć pracę, wpisz `exit` w konsoli serwera.

## Format pliku .dataEnv

Każda wartość zajmuje osobny wiersz. Kolejność ma znaczenie. Plik zawiera same wartości, bez nazw kluczy, znaków `=`, komentarzy i dodatkowych pustych wierszy. Nazwę pliku przekazujesz jako pierwszy argument do `mainApp`.

```text
56571
127.0.0.1
portal_demo
portal_user
twoje_haslo
5432
https://example.org
```

| Wiersz | Klucz w kodzie | Znaczenie |
| --- | --- | --- |
| 1 | `listenPort` | Port nasłuchiwania HTTP |
| 2 | `host` | Adres serwera PostgreSQL |
| 3 | `base_name` | Nazwa bazy danych |
| 4 | `user` | Użytkownik bazy |
| 5 | `password` | Hasło użytkownika bazy |
| 6 | `base_port` | Port PostgreSQL |
| 7 | `homePage_url` | Adres wyświetlany jako odnośnik na stronie powitalnej |

Port HTTP i adres odnośnika są wykorzystywane także w trybie podstawowym. Wartość `host` dotyczy wyłącznie połączenia z bazą. Adres interfejsu HTTP ustawia konstruktor `httpServer`.

## Gdzie dodać endpoint

| Plik | Rola |
| --- | --- |
| [mainApp.java](mainApp.java) | Wczytuje konfigurację, uruchamia wątek nasłuchujący i obsługuje polecenie `exit`. |
| [httpServer.java](httpServer/httpServer.java) | Tworzy `ServerSocket`, przyjmuje połączenia i uruchamia osobny wątek dla każdego zaakceptowanego połączenia. |
| [requestExecute.java](httpServer/requestExecute.java) | Odczytuje żądanie z gniazda klienta, wywołuje parser i wybiera klasę obsługi na podstawie metody HTTP. |
| [requestParser.java](httpServer/requestParser.java) | Rozdziela linię żądania, ścieżkę, parametry URL i nagłówki. Zawiera także kod odczytu treści i obiektu JSON przez `javax.json`. |
| [getRoute.java](route/getRoute.java) | Obsługuje `GET /`. Tu można dodać kolejne ścieżki GET. |
| [postRoute.java](route/postRoute.java) | Miejsce na endpointy POST. W tej wersji klasa zawiera domyślną odpowiedź `404`. |
| [sqlModule.java](sqlModule/sqlModule.java) | Tworzy pulę HikariCP i udostępnia przykładowe zapytanie `getHotAuction`. |

**GET.** Dodaj `case` dla swojej ścieżki w `getRoute.route(...)` i skieruj go do metody zapisującej odpowiedź do gniazda klienta. Za wzór służy `getMain(...)`. Parametry adresu są dostępne w mapie `params`, np. dla `/?lang=pl` klucz `lang` ma wartość `pl`. Sama strona powitalna wyświetla tę samą treść.

**POST.** W [requestExecute.java](httpServer/requestExecute.java) za wybór metody HTTP odpowiada `switch (reqParser.method)` wewnątrz `run()`. Poniższa gałąź przekazuje żądanie do istniejącej klasy `postRoute`, której konstruktor przyjmuje gniazdo klienta. Jeśli w swojej wersji masz tylko `case "GET"`, wstaw ten fragment tuż przed `default`.

```java
case "POST":
{
    postRoute postR = new postRoute(clientSock);
    postR.route(reqParser.path, reqParser.urlParams, reqParser.headers,
                reqParser.buffer, reqParser.jsonObj);
    break;
}
```

Po tej zmianie żądania POST trafiają do [postRoute.java](route/postRoute.java). W metodzie `route(...)` dodaj osobny `case` w `switch (path)` dla każdego endpointu. Dopóki przełącznik zawiera tylko `default`, żądanie przekazane do tej klasy otrzyma odpowiedź `404`.

Jeżeli endpoint ma przyjmować JSON, sprawdź również weryfikację treści w `requestParser`. Obecny warunek `content_length <= buffer.length()` odrzuca także treść o dokładnie zadeklarowanej długości. Nagłówek `Content-Length` określa liczbę bajtów, a `String.length()` liczy jednostki UTF-16.

## Dodanie obsługi PostgreSQL

1. Przygotuj działającą bazę PostgreSQL i konto z dostępem do niej. Wpisz dane połączenia w wierszach 2–6 pliku konfiguracyjnego. W paczce są już sterownik JDBC PostgreSQL, HikariCP i SLF4J.
2. W konstruktorze klasy `httpServer` odkomentuj instrukcję, która tworzy pulę połączeń.

   ```java
   sql_module = new sqlModule(dataConf);
   ```

3. W metodzie `exit()` tej samej klasy odkomentuj wywołanie zamykające pulę.

   ```java
   sql_module.close();
   ```

4. Ponownie skompiluj i uruchom program. Moduł buduje adres w formacie `jdbc:postgresql://host:port/baza` i inicjalizuje pulę jeszcze przed rozpoczęciem nasłuchiwania HTTP. Przy błędnych danych lub niedostępnej bazie uruchomienie w tym trybie może się zakończyć na tym etapie.

### Przykład z portalu – najbliższa aukcja

Metoda `getHotAuction(Map<String, String> auctionObj)` pobiera połączenie z puli i wyszukuje najmniejszą wartość `auction_time` większą od bieżącego czasu. Dla znalezionego terminu odczytuje z tabeli `auctions` kolumny `auction_id`, `job_title` i `auction_time`. Czas w tabeli reprezentuje liczbę **milisekund od epoki Unix**. Konto używane przez serwer potrzebuje uprawnienia do odczytu tabeli.

Po włączeniu SQL możesz użyć tej metody w klasie `getRoute`. W poniższym przykładzie pobierany jest tytuł aukcji.

```java
Map<String, String> auction = new HashMap<>();
int result = httpServ.sql_module.getHotAuction(auction);

if (result == 0 && !auction.get("auction_id").isEmpty()) {
    String title = auction.get("job_title");
    // Użyj tytułu przy budowaniu odpowiedzi dla strony głównej.
}
```

| Wynik | Zawartość mapy |
| --- | --- |
| `0` i znaleziono aukcję | Identyfikator, tytuł i czas aukcji w postaci tekstu |
| `0` i brak przyszłej aukcji | Wszystkie trzy pola zawierają puste ciągi |
| `1` | Wystąpił błąd SQL. Kod wywołujący może zwrócić odpowiedź o błędzie. |

W portalu wynik trafiał na stronę główną podczas jej ładowania. W tej wersji metoda pozostaje przykładem do wykorzystania we własnej trasie. Samo włączenie puli pozostawia stronę powitalną pod `GET /`.
