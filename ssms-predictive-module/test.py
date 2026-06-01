import requests

url = "http://localhost:9091/api/predictive/analyze-file"
files = {'file': ('test.txt', 'This is a test student report', 'text/plain')}
try:
    response = requests.post(url, files=files)
    print(f"Status Code: {response.status_code}")
    print(f"Response Body: {response.text}")
except Exception as e:
    print(f"Error: {e}")
