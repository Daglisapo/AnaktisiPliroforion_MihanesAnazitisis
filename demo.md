# Σενάριο ζωντανής παρουσίασης (~5 λεπτά)

**Πριν το μάθημα** (το App 1 στα 10.000 PDF και το App 2 κάνουν ώρα — τα τρέχουμε από πριν):

```bat
build.bat
run-app1.bat
run-app2.bat
```

Ανοίγουμε τρία παράθυρα cmd στον φάκελο του project και το VS Code με τον κώδικα.

---

### 0:00 – 0:45 · Αρχιτεκτονική

Δείχνουμε το σχήμα του README: 5 apps, 5 packages στο VS Code (`store`, `indexer`, `server`, `client`, `ui`).
«Γράφτηκε σε VS Code, Java 21, Lucene 10.6, Maven.»

### 0:45 – 1:30 · App 1 + App 2 (έτοιμα από πριν)

```bat
inspect-repository.bat
inspect-index.bat network retrieval learning --top 10
analyze.bat "Neural Networks and networking: the network's evaluation"
```

- Δείχνουμε τα `repo_000xx.jsonl` (~128 MB) και μία γραμμή JSON.
- Δείχνουμε df/cf όρων και ότι `networks → network` (stemming).
- Στο VS Code: `LuceneDocumentFactory.CONTENT_TYPE` (DOCS_AND_FREQS_AND_POSITIONS + stored).

### 1:30 – 2:30 · App 3 + App 4

Παράθυρο 1:

```bat
run-app3.bat
```

Παράθυρο 2:

```bat
run-app4.bat "machine learning" 5
run-app4.bat
```

Στο διαδραστικό prompt:

```
Ερώτημα> "neural network"
Ερώτημα> retrieval AND evaluation
Ερώτημα> graph OR network NOT neural
Ερώτημα> neural AND (
```

Το τελευταίο δείχνει ότι λάθος σύνταξη → `{"error": ...}` και ο server συνεχίζει (δείχνουμε το log του server).

### 2:30 – 4:00 · App 5

Παράθυρο 3:

```bat
run-app5.bat
```

Browser → <http://localhost:8080>. Ερωτήματα:

1. `"information retrieval"` — φράση (positions)
2. `reinforcement learning AND robot` — Boolean AND
3. `graph OR network NOT neural` — OR / NOT
4. `transform* AND translation` — wildcard
5. `quantum` — απλό ερώτημα, σελιδοποίηση («Επόμενη»)

Κλικ σε έναν τίτλο → ανοίγει το PDF. Δείχνουμε score, snippet με τονισμένους όρους, πλήθος αποτελεσμάτων.

### 4:00 – 4:30 · Ανθεκτικότητα

- Στο παράθυρο 2: `run-app4.bat --shutdown` → ο server τερματίζει (μόνο έτσι βγαίνει από το loop).
- Στον browser νέα αναζήτηση → μήνυμα «Δεν ήταν δυνατή η επικοινωνία με τον Query Server… Τρέχει το App 3;».
- Δείχνουμε το `C:\IR_data\repository\errors.log` (χαλασμένα PDF που παραλείφθηκαν).

### 4:30 – 5:00 · Ερωτήσεις

Βλ. την ενότητα «Πιθανές ερωτήσεις» στο ΕΞΗΓΗΣΗ.md.
