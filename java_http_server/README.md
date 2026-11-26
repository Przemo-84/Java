# java_http_server

Moduł do budowy API w Javie na Linuksie. Przyjmuje żądania HTTP przez TCP i rozdziela obsługę metod GET i POST między dwie klasy tras. Powstał jako część większego portalu. Zawiera opcjonalny moduł PostgreSQL z pulą połączeń HikariCP.

## Wdrożenie na Linuksie

1. Przygotuj JDK 11 lub nowszy oraz Node.js z npm. Skopiuj katalog projektu na serwer razem z katalogiem `jar/`, który zawiera zależności. Polecenia wykonuj w katalogu z `mainApp.java` i `package.json`.
2. W tym samym katalogu przygotuj plik `.dataEnv`. Wpisz po jednej wartości w wierszu, w poniższej kolejności.

   ```text
   56571
   127.0.0.1
   portal_demo
   portal_user
   haslo_uzytkownika
   5432
   https://example.org
   ```

   | Wiersz | Wartość |
   | --- | --- |
   | 1 | Port HTTP |
   | 2–6 | Host PostgreSQL, nazwa bazy, użytkownik, hasło i port bazy |
   | 7 | Adres strony wyświetlany na stronie powitalnej |

   Przy wyłączonym module SQL pozostaw przykładowe wartości w wierszach 2–6, aby zachować pozycję adresu strony.

3. Skompiluj i uruchom aplikację.

   ```bash
   npm run linux_compile
   npm run linux_start
   ```

   W PowerShellu i CMD odpowiednie polecenia to `npm run compile` i `npm run start`.

Serwer nasłuchuje na `127.0.0.1`. Dla konfiguracji powyżej lokalny adres to `http://127.0.0.1:56571`. Jeśli używasz Nginx na tym samym hoście, ustaw ten adres jako cel `proxy_pass`. Adres nasłuchu ustawia konstruktor w [httpServer.java](httpServer/httpServer.java). Żeby zatrzymać serwer, wpisz `exit` w jego konsoli.

## Struktura projektu

| Plik lub katalog | Odpowiedzialność |
| --- | --- |
| [mainApp.java](mainApp.java) | Wczytuje konfigurację, uruchamia serwer i obsługuje polecenie `exit`. |
| [httpServer/httpServer.java](httpServer/httpServer.java) | Otwiera gniazdo nasłuchujące i uruchamia osobny wątek dla każdego przyjętego połączenia. |
| [httpServer/requestExecute.java](httpServer/requestExecute.java) | Odczytuje żądanie porcjami, wywołuje parser i wybiera klasę trasy według metody HTTP. |
| [httpServer/requestParser.java](httpServer/requestParser.java) | Rozdziela ścieżkę, parametry URL, nagłówki i body. Odczytuje obiekt JSON. |
| [route/getRoute.java](route/getRoute.java) i [route/postRoute.java](route/postRoute.java) | Zawierają endpointy GET i POST oraz generują odpowiedzi. |
| [sqlModule/sqlModule.java](sqlModule/sqlModule.java) | Zarządza pulą połączeń PostgreSQL i zawiera przykładowe zapytanie `getHotAuction()`. |
| [jar/](jar/) i [package.json](package.json) | Biblioteki Javy oraz polecenia kompilacji i uruchamiania. |

## Obsługa żądań i dodawanie endpointów

`requestExecute` przekazuje każdą odczytaną porcję do `requestParser`. Gdy parser potrzebuje dalszych danych, wątek kontynuuje odczyt. Po skompletowaniu żądania wywołuje `route(reqParser)` w klasie trasy. Limit pojedynczego żądania wynosi 15 000 elementów `char`, a timeout odczytu z gniazda wynosi 200 ms.

Każdy `POST` wymaga nagłówków `Content-Type` i `Content-Length`. Długość podaj w bajtach. Dla pustego body ustaw `Content-Length: 0`. Aby przekazać obiekt JSON, użyj `Content-Type: application/json`.

Nową ścieżkę dodaj jako `case` w `switch (reqParser.path)` w metodzie `route()`. Wybierz `getRoute` dla GET albo `postRoute` dla POST. W trasie masz dostęp do następujących danych.

| Dane | Dostęp w trasie |
| --- | --- |
| Ścieżka i parametry URL | Dla `GET /?lang=pl` pole `reqParser.path` zawiera `/`, a `reqParser.urlParams.get("lang")` zwraca `"pl"`. |
| Nagłówki | `reqParser.headers`, np. `reqParser.headers.get("Content-Type")`. |
| Body i obiekt JSON | `reqParser.bodyChar` zawiera treść body, a `reqParser.jsonObj` odczytany obiekt JSON. Przy pustym body oba pola mają wartość `null`. |

W wersji podstawowej `GET /` i poprawnie sformułowany `POST /` zwracają stronę powitalną. Dla nieznanej ścieżki obie klasy zwracają `404`.

## Włączenie PostgreSQL

1. Utwórz bazę i użytkownika, nadaj mu uprawnienia do używanych tabel i wpisz dane połączenia w wierszach 2–6 pliku `.dataEnv`.
2. W [httpServer.java](httpServer/httpServer.java) odkomentuj `sql_module = new sqlModule(dataConf);` w konstruktorze oraz `sql_module.close();` w metodzie `exit()`.
3. Ponownie skompiluj i uruchom serwer. Aplikacja inicjalizuje pulę HikariCP przed rozpoczęciem nasłuchu.


Z poziomu trasy moduł jest dostępny jako `httpServ.sql_module`. Metoda `getHotAuction(Map<String, String> auctionObj)` pokazuje, jak pobrać połączenie z puli i wykonać zapytanie. W tabeli `auctions` wybiera najbliższą przyszłą aukcję według `auction_time` (milisekundy od epoki Unix) i wpisuje do mapy `auction_id`, `job_title` oraz `auction_time`. Gdy brak przyszłej aukcji, wartości w mapie są pustymi ciągami. Metoda zwraca `0` po wykonaniu zapytania lub `1` przy błędzie SQL. 
Jest to metoda, która zostala dodana jako przykład obslugi żądania do bazy.
