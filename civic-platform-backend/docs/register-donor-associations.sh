#!/usr/bin/env bash
# Register multiple mock associations (DONOR) against POST /api/auth/register
# Usage: ./register-donor-associations.sh
# Requires: backend running (default http://localhost:8081/api)

BASE="${API_BASE:-http://localhost:8081/api}"

register() {
  local name="$1"
  echo "=== Registering: $name ==="
  curl -sS -w "\nHTTP %{http_code}\n" -X POST "${BASE}/auth/register" \
    -H "Content-Type: application/json" \
    -d "$2"
  echo ""
}

register "GreenFork Food Bank" '{
  "userName": "greenfork_admin",
  "email": "admin@greenfork-food.org",
  "password": "SecurePass1!",
  "userType": "DONOR",
  "firstName": "Amélie",
  "lastName": "Bernard",
  "phone": "+33 6 12 34 56 78",
  "address": "12 Rue des Lilas, 75011 Paris, France",
  "associationName": "GreenFork Food Bank",
  "companyName": "GreenFork Association",
  "contactName": "Amélie Bernard",
  "contactEmail": "contact@greenfork-food.org"
}'

register "Urban Garden Collective" '{
  "userName": "urban_garden_ops",
  "email": "ops@urbangarden.lyon.fr",
  "password": "SecurePass2!",
  "userType": "DONOR",
  "firstName": "Karim",
  "lastName": "Haddad",
  "phone": "+33 4 78 90 12 34",
  "address": "45 Cours Gambetta, 69007 Lyon, France",
  "associationName": "Urban Garden Collective",
  "companyName": "Jardins Urbains SAS",
  "contactName": "Karim Haddad",
  "contactEmail": "contact@urbangarden.lyon.fr"
}'

register "Coastal Cleanup Crew" '{
  "userName": "coastal_cc_marie",
  "email": "marie.dupont@coastalcleanup.fr",
  "password": "SecurePass3!",
  "userType": "DONOR",
  "firstName": "Marie",
  "lastName": "Dupont",
  "phone": "+33 6 98 76 54 32",
  "address": "8 Quai des Docks, 13002 Marseille, France",
  "associationName": "Coastal Cleanup Crew",
  "companyName": "CCC Environnement",
  "contactName": "Marie Dupont",
  "contactEmail": "partners@coastalcleanup.fr"
}'

register "Youth Code Lab" '{
  "userName": "ycl_toulouse",
  "email": "hello@youthcodelab.org",
  "password": "SecurePass4!",
  "userType": "DONOR",
  "firstName": "Lucas",
  "lastName": "Moreau",
  "phone": "+33 5 61 22 33 44",
  "address": "22 Allée Jean Jaurès, 31000 Toulouse, France",
  "associationName": "Youth Code Lab",
  "companyName": "YCL Éducation Numérique",
  "contactName": "Lucas Moreau",
  "contactEmail": "contact@youthcodelab.org"
}'

register "Refugee Welcome Network" '{
  "userName": "rwn_strasbourg",
  "email": "coord@refugeewelcome-net.eu",
  "password": "SecurePass5!",
  "userType": "DONOR",
  "firstName": "Sofia",
  "lastName": "Petrov",
  "phone": "+33 3 88 11 22 33",
  "address": "5 Place Kléber, 67000 Strasbourg, France",
  "associationName": "Refugee Welcome Network",
  "companyName": "RWN Solidarité",
  "contactName": "Sofia Petrov",
  "contactEmail": "partners@refugeewelcome-net.eu"
}'

echo "Done. If you see 409, emails/usernames already exist — change them or clear DB."
