import jwt
import time
from datetime import datetime, timedelta

PRIVATE_KEY_PATH = "../../infra/secrets/private.pem" 

with open(PRIVATE_KEY_PATH, "r") as f:
    private_key = f.read()

now = datetime.utcnow()
payload = {
    "sub": "streaming-token",
    "iss": "netflix-backend",
    "iat": now,
    "exp": now + timedelta(minutes=15),
    "movieId": "sintel",       
    "ip": "127.0.0.1"          
}

token = jwt.encode(payload, private_key, algorithm="RS256")
print("\n--- TON TICKET SÉCURISÉ ---")
print(token)
print("---------------------------\n")