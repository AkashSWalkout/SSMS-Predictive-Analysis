import requests
import json

url = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
headers = {
    "Authorization": "Bearer AIzaSyDIBpLd547b7H1agpeBgT7-ZvgmwLfDY00",
    "Content-Type": "application/json"
}
payload = {
    "model": "gemini-1.5-flash-latest",
    "messages": [
        {"role": "user", "content": "Hello!"}
    ]
}

response = requests.post(url, headers=headers, json=payload)
print(f"Status Code: {response.status_code}")
print(f"Response: {response.text}")
