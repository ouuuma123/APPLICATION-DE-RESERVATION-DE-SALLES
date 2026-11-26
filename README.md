# Roomly

Application web de réservation de salles de réunion. Les employés consultent les salles, les réservent et suivent leurs réservations. Les administrateurs gèrent les salles, les utilisateurs et l'ensemble des réservations.

Le projet se compose d'un frontend Angular et d'une API REST Spring Boot, avec une base H2 en mémoire.

```text
Reservation_Salles/
├── frontend_reservations_salles/   # Angular
└── backend_reservations_salles/    # Spring Boot
```

## Fonctionnalités

- Connexion et inscription (JWT). Un nouvel inscrit reçoit le rôle `EMPLOYEE`.
- Recherche de salles disponibles par capacité, localisation ou équipement
- Réservation d'un créneau, avec contrôle des conflits et de la capacité
- Modification et annulation des réservations
- Vues par jour, semaine et mois
- Tableau de bord avec les prochaines réunions et des statistiques
- Notifications internes : confirmation, modification, annulation et rappel
- Gestion des profils utilisateurs
- Administration des salles (dont activation et désactivation), des utilisateurs, des rôles et des réservations

Les rôles disponibles sont `ADMIN`, `EMPLOYEE` et `CLIENT`. L'interface est responsive et la navigation s'adapte aux rôles `EMPLOYEE` et `ADMIN`.

## Stack technique

**Frontend :** Angular 22, TypeScript 6, Angular Material, Angular Router, RxJS, SCSS, Vitest

**Backend :** Java 21, Spring Boot 3.2 (Web, Security, OAuth2 Resource Server, Data JPA), JWT, Jakarta Validation, Thymeleaf, H2, Lombok, Maven

## Installation

Prérequis : Java 21, Maven, Node.js et npm.

### Backend

```bash
cd backend_reservations_salles
mvn spring-boot:run
```

L'API est accessible sur `http://localhost:8081`.

### Frontend

```bash
cd frontend_reservations_salles
npm install
npm start
```

L'interface est accessible sur `http://localhost:4200`.

## Configuration

Par défaut, le frontend tourne en mode démonstration avec des données locales. Pour le connecter à l'API, passez `DEMO_MODE` à `false` dans `src/app/core/api.config.ts` :

```typescript
export const DEMO_MODE = false;
export const API_BASE_URL = 'http://localhost:8081/api';
```

## Base de données

La base H2 est en mémoire : les données sont recréées à chaque démarrage. La console est disponible sur `http://localhost:8081/h2-console`.

| Paramètre    | Valeur               |
|--------------|----------------------|
| JDBC URL     | `jdbc:h2:mem:testdb` |
| Utilisateur  | `sa`                 |
| Mot de passe | (vide)               |

Au démarrage, le backend insère des données de test : 4 salles, plusieurs utilisateurs et quelques réservations. 
Un compte administrateur de démonstration est créé également :

- Email : `admin@company.com`
- Mot de passe : `admin123`

## Tests

Backend (tests unitaires, d'intégration, REST et Selenium) :

```bash
cd backend_reservations_salles
mvn test
```

Frontend (tests puis build) :

```bash
cd frontend_reservations_salles
npm test -- --watch=false
npm run build
```