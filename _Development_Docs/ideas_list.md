3.12.2013
[IN CZECH]
not now: Stav ulohy - uloha muze behem vypoctu nastavit, kolik asi dat je spocitano. Server si pak o tuto informaci muze rict, pokud se ho klient zepta, kolik uz je celkove spocitano + k teto informaci pricte vsechny celkove spocitane tasky. Misto is downloadReady pak muze byt metoda, ktera se pta na aktualni stav vypoctu a klient ho muze oznamit. Stejne tak pri listovani uloh.

Pametova narocnost - jvm se spousti s nastavenou velikosti pameti, to znamena ze z konfiguraku uz to pujde asi dost tezko ovlivnit. To znamena ze toto by to chtelo poresit (nejaky startovaci script?)
 - nejake ulohy mohou vyzadovat vice pameti nez kolik mame k dispozici. Bylo by dobre u kazde ulohy nekde v manifestu specifikovat kolik bude potrebovat pameti. Server pak podle toho kolik klient ma volne pameti bud zasle ulohu nebo ne.

important: Jak je to s podporou vice uloh najednou? Pokud dovolim vice uloh, tak by kazda uloha mela specifikovat, kolik jader a kolik pameti bude nejvice vyzadovat. Nebude se tedy zjistovat kolik pameti je ted zrovna k dispozici ale rezervovat pamet dopredu.


- make an table : user name -> unique id
  then the user is able to change name, but file structure on server will remain same
- make client able to choose where the log files are placed ?