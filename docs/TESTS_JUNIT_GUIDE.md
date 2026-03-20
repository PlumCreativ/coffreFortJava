# Tests Unitaires JUnit 5 - CoffreFort JavaFX

## Vue d'ensemble

Ce document détaille les tests unitaires JUnit 5 générés pour les contrôleurs du projet CoffreFort, une application JavaFX de coffre-fort numérique. Les tests suivent la structure **AAA** (Arrange/Act/Assert) et utilisent **Mockito 5.5.0** pour mocker les dépendances.

---

## 1. Configuration et Dépendances

### Dépendances ajoutées au `pom.xml`

```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.5.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.5.0</version>
    <scope>test</scope>
</dependency>
```

### Annotations JUnit 5 utilisées

- `@ExtendWith(MockitoExtension.class)` : Intègre Mockito avec JUnit 5
- `@DisplayName()` : Fournit des noms de tests explicites en français
- `@Test` : Marque une méthode comme test unitaire
- `@BeforeEach` : Initialise l'état avant chaque test
- `@ParameterizedTest` : Teste plusieurs valeurs avec `@ValueSource`

---

## 2. Tests de CreateFolderController

### Fichier : `CreateFolderControllerTest.java`

**Objectif** : Tester la création de dossiers avec validation du nom.

### Scénarios testés

#### 2.1 Cas Nominal (Happy Path)
| Scénario | Test | Résultat attendu |
|----------|------|------------------|
| Nom valide simple | `testCreateFolderWithValidName()` | ✅ Dossier créé, callback exécuté, fenêtre fermée |
| Nom d'un caractère | `testCreateFolderWithSingleCharName()` | ✅ Création réussie |
| Nom avec espaces trimés | `testCreateFolderWithTrimmedSpaces()` | ✅ Espaces supprimés, création réussie |
| Nom max 50 caractères | `testCreateFolderWithMaxLengthName()` | ✅ Création réussie |
| Nom avec nombres | `testCreateFolderWithNumbers()` | ✅ Création réussie |
| Nom avec tirets/underscores | `testCreateFolderWithDashesAndUnderscores()` | ✅ Création réussie |
| Nom avec caractères accentués | `testCreateFolderWithAccentedCharacters()` | ✅ Création réussie |

#### 2.2 Cas d'Erreur : Validation du nom

| Scénario | Test | Erreur attendue |
|----------|------|-----------------|
| Nom vide | `testCreateFolderWithEmptyName()` | "Le nom du dossier ne peut pas être vide." |
| Nom null | `testCreateFolderWithNullName()` | "Le nom du dossier ne peut pas être vide." |
| Espaces uniquement | `testCreateFolderWithOnlySpaces()` | "Le nom du dossier ne peut pas être vide." |
| Plus de 50 caractères | `testCreateFolderWithNameTooLong()` | "Le nom est trop long (maximum 50 caractères)." |
| Contient `\` | `testCreateFolderWithInvalidCharacters()` | "Le nom du dossier contient des caractères invalides..." |
| Contient `/` | (ParameterizedTest) | Même message d'erreur |
| Contient `:*?"<>\|` | (ParameterizedTest) | Même message d'erreur |
| Caractères invalides multiples | `testCreateFolderWithMultipleInvalidCharacters()` | Erreur caractères invalides |

#### 2.3 Cas d'Erreur : Gestion des ressources

| Scénario | Test |
|----------|------|
| Callback `onCreateFolder` null | `testCreateFolderWithNullCallback()` |
| Stage `dialogStage` null | `testCreateFolderWithNullStage()` |

#### 2.4 Cas : Annulation

| Scénario | Test |
|----------|------|
| Annulation avec callback | `testCancelFolderCreation()` |
| Annulation sans callback | `testCancelFolderCreationWithoutCallback()` |
| Annulation sans stage | `testCancelFolderCreationWithoutStage()` |

---

## 3. Tests de LoginController

### Fichier : `LoginControllerTest.java`

**Objectif** : Tester l'authentification avec validation d'email et mot de passe.

### Scénarios testés

#### 3.1 Cas Nominal (Happy Path)

| Scénario | Test | Résultat attendu |
|----------|------|------------------|
| Validation champs non vides | `testValidateRequiredFieldsNotEmpty()` | ✅ Validation réussie |
| Email format standard | `testValidateEmailFormatCorrect()` | ✅ Format valide (regex) |
| Email avec plus signe | `testEmailWithPlusSign()` | ✅ Format valide |
| Email avec domaines multiples | `testEmailWithSubdomains()` | ✅ Format valide |
| Mot de passe spécial | `testPasswordWithSpecialCharacters()` | ✅ Accepté |

#### 3.2 Cas d'Erreur : Champs obligatoires

| Scénario | Test | Erreur attendue |
|----------|------|-----------------|
| Email vide | `testLoginWithEmptyEmail()` | "Veuillez saisir l'email et le mot de passe." |
| Mot de passe vide | `testLoginWithEmptyPassword()` | Même message |
| Les deux vides | `testLoginWithEmptyEmailAndPassword()` | Même message |

#### 3.3 Cas d'Erreur : Format email invalide

| Scénario | Test |
|----------|------|
| Pas de symbole `@` | `testLoginWithInvalidEmailFormats()` (ParameterizedTest) |
| `@` sans domaine | (ParameterizedTest) |
| Espace dans email | (ParameterizedTest) |
| Format incomplet | (ParameterizedTest) |
| Double `@@` | (ParameterizedTest) |

#### 3.4 Cas d'Erreur : Sécurité

| Scénario | Test | Erreur attendue |
|----------|------|-----------------|
| Mot de passe visible | `testLoginWithPasswordVisible()` | "Veuillez masquer le mot de passe avant de vous connecter." |
| ApiClient non initialisé | `testLoginWithoutApiClient()` | Pas d'appel API |

#### 3.5 Cas : Toggle Affichage/Masquage du mot de passe

| Scénario | Test | Effet attendu |
|----------|------|----------------|
| Cocher "Afficher" | `testToggleShowPasswordWhenChecked()` | Champ visible, PasswordField caché |
| Décocher | `testToggleShowPasswordWhenUnchecked()` | PasswordField visible, champ caché |
| Curseur repositionné | `testToggleShowPasswordCursorPosition()` | Curseur à la fin du texte |

#### 3.6 Cas : Navigation

| Scénario | Test |
|----------|------|
| Callback onSuccess | `testLoginSuccessCallback()` |
| Navigation S'inscrire | `testGoToRegisterNavigation()` |
| Navigation Mot de passe oublié | `testForgotPasswordNavigation()` |

#### 3.7 Cas Limites : Espaces

| Scénario | Test |
|----------|------|
| Email avec espaces | `testLoginWithEmailSpaces()` |
| Mot de passe avec espaces | `testLoginWithPasswordSpaces()` |

#### 3.8 Cas : Setters et Injection

| Scénario | Test |
|----------|------|
| Setter ApiClient | `testSetAndGetApiClient()` |
| Setter dialogStage | `testSetDialogStage()` |
| Setter onSuccess | `testSetOnSuccess()` |
| Setter onGoToRegister | `testSetOnGoToRegister()` |

---

## 4. Tests de ConfirmDeleteController

### Fichier : `ConfirmDeleteControllerTest.java`

**Objectif** : Tester les dialogues de confirmation de suppression.

### Scénarios testés

#### 4.1 Cas Nominal : Confirmation

| Scénario | Test |
|----------|------|
| Confirmation avec callback | `testConfirmDeleteWithCallback()` |
| Fermeture sans callback | `testConfirmDeleteWithoutCallback()` |
| Ordre : callback puis fermeture | `testConfirmDeleteCallbackBeforeClose()` |

#### 4.2 Cas Nominal : Annulation

| Scénario | Test |
|----------|------|
| Annulation avec callback | `testCancelDeleteWithCallback()` |
| Fermeture sans callback | `testCancelDeleteWithoutCallback()` |
| Ordre : callback puis fermeture | `testCancelDeleteCallbackBeforeClose()` |

#### 4.3 Cas : Affichage du message

| Scénario | Test |
|----------|------|
| Message normal | `testSetMessage()` |
| Message null | `testSetMessageWithNull()` |
| Message vide | `testSetMessageWithEmptyString()` |
| Message avec caractères spéciaux | `testSetMessageWithSpecialCharacters()` |
| Message très long | `testSetMessageWithLongText()` |

#### 4.4 Cas : Affichage du nom du fichier

| Scénario | Test |
|----------|------|
| Nom simple | `testSetFileName()` |
| Nom null | `testSetFileNameWithNull()` |
| Nom vide | `testSetFileNameWithEmptyString()` |
| Nom avec extension | `testSetFileNameWithExtension()` |
| Nom avec chemin | `testSetFileNameWithPath()` |
| Nom accentué | `testSetFileNameWithAccents()` |

#### 4.5 Cas Limites : Stage null

| Scénario | Test |
|----------|------|
| Confirmation sans stage | `testConfirmDeleteWithoutStage()` |
| Annulation sans stage | `testCancelDeleteWithoutStage()` |

#### 4.6 Cas : Interactions multiples

| Scénario | Test |
|----------|------|
| Confirmations successives | `testMultipleConfirmations()` |
| Changement message après initial | `testChangeMessageAfterInitial()` |
| Changement nom après initial | `testChangeFileNameAfterInitial()` |

#### 4.7 Cas : Indépendance des callbacks

| Scénario | Test |
|----------|------|
| onConfirm et onCancel indépendants | `testIndependentCallbacks()` |

#### 4.8 Cas : NULL Safety

| Scénario | Test |
|----------|------|
| messageLabel null | `testSetMessageWithNullLabel()` |
| fileNameLabel null | `testSetFileNameWithNullLabel()` |

---

## 5. Structure des Tests

### Pattern AAA (Arrange-Act-Assert)

Tous les tests suivent ce pattern :

```java
@Test
@DisplayName("Cas de test explicite")
void testSomething() {
    // Arrange - Préparation des données et mocks
    String input = "valeur";
    when(mockObject.method()).thenReturn(value);
    
    // Act - Exécution de la fonction testée
    controller.handleAction(input);
    
    // Assert - Vérification des résultats
    verify(mockObject).method();
    assertEquals(expected, actual);
}
```

### Injection de dépendances par réflexion

Pour tester les contrôleurs JavaFX sans modifier leur code :

```java
private void injectField(String fieldName, Object value) {
    try {
        var field = ControllerClass.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException("Erreur lors de l'injection: " + fieldName, e);
    }
}
```

---

## 6. Exécution des tests

### Via Maven

```bash
# Tous les tests
mvn test

# Tests d'une classe spécifique
mvn test -Dtest=CreateFolderControllerTest

# Tests avec un pattern spécifique
mvn test -Dtest=*ControllerTest

# Avec rapport de couverture
mvn test jacoco:report
```

### Via IDE (IntelliJ)

1. Click droit sur la classe de test → "Run 'TestClassName'"
2. Click droit sur une méthode de test → "Run 'testMethodName()'"
3. Ctrl+Shift+F10 (Windows/Linux) ou Cmd+Shift+R (Mac)

---

## 7. Métriques de couverture

### CreateFolderControllerTest
- **Méthodes testées** : `initialize()`, `handleCreate()`, `handleCancel()`, `isValidFolderName()`
- **Cas testés** : 26 scénarios
- **Couverture estimée** : ~95%

### LoginControllerTest
- **Méthodes testées** : `handleLogin()`, `handleToggleShowPassword()`, `handleGoToRegister()`
- **Cas testés** : 28 scénarios
- **Couverture estimée** : ~85%

### ConfirmDeleteControllerTest
- **Méthodes testées** : `handleConfirm()`, `handleCancel()`, `setMessage()`, `setFileName()`, `close()`
- **Cas testés** : 31 scénarios
- **Couverture estimée** : ~90%

**Total : 85 tests pour 3 contrôleurs**

---

## 8. Best Practices appliquées

### ✅ Bonnes pratiques

1. **Noms explicites** : Chaque test a un `@DisplayName` en français décrivant le comportement attendu
2. **Structure AAA** : Arrange → Act → Assert, clairement séparées
3. **Un test = un comportement** : Chaque test valide un seul aspect
4. **Mocking des dépendances** : Utilisation de Mockito pour isoler la classe testée
5. **Tests paramétrés** : `@ParameterizedTest` pour tester plusieurs valeurs
6. **Pas de dépendances externes** : Tests rapides et répétables
7. **Documentation** : Javadoc et commentaires explicatifs
8. **Verifications précises** : `verify()`, `assertThrows()`, `assertEquals()`

### 🚫 Anti-patterns évités

- ❌ Tests dépendant les uns des autres
- ❌ Ordre d'exécution important
- ❌ Pas de nettoyage après les tests
- ❌ Assertions multiples sans raison
- ❌ Tests trop complexes ou trop longs

---

## 9. Exemple : Ajouter un nouveau test

Pour tester un nouveau contrôleur, suivez ce template :

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("NewController Tests")
class NewControllerTest {
    
    private NewController controller;
    
    @Mock private Label mockLabel;
    
    @BeforeEach
    void setUp() {
        controller = new NewController();
        injectField("labelField", mockLabel);
    }
    
    private void injectField(String fieldName, Object value) {
        try {
            var field = NewController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(controller, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    @DisplayName("Cas de test explicite")
    void testBehavior() {
        // Arrange
        when(mockLabel.getText()).thenReturn("test");
        
        // Act
        controller.someMethod();
        
        // Assert
        verify(mockLabel).setText(anyString());
    }
}
```

---

## 10. Dépannage

### Test échoue : "Field not found"
**Cause** : Le nom du champ ne correspond pas à la classe testée.
**Solution** : Vérifier le nom exact du champ dans la classe source.

### Test échoue : "NullPointerException"
**Cause** : Un mock n'a pas été correctement injecté.
**Solution** : Vérifier que `injectField()` est appelé dans `@BeforeEach`.

### Mock ne fait rien
**Cause** : Oubli de configurer le mock avec `when()`.
**Solution** : Configurer tous les comportements attendus dans "Arrange".

---

## Conclusion

Cette suite de tests JUnit 5 fournit une couverture robuste des contrôleurs CoffreFort avec :
- ✅ 85 tests au total
- ✅ Structure AAA systématique
- ✅ Mockito pour l'isolation
- ✅ Cas nominal, erreur et limites
- ✅ Noms explicites en français
- ✅ Documentation complète

Les tests sont maintenables, évolutifs et prêts pour l'intégration continue.
