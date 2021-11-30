import requests
from bs4 import BeautifulSoup
import csv

r = requests.get("http://flavorsofcacao.com/database_w_REF.html")
soup = BeautifulSoup(r.content, "html.parser")

with open('foc.csv', 'w', newline='', encoding='utf-8') as f_output:
    csv_output = csv.writer(f_output)
    csv_output.writerow([th.get_text(strip=True) for th in soup.table.tr.find_all('th')])

    for tr in soup.table.find_all("tr")[1:]:
        csv_output.writerow([td.get_text(strip=True) for td in tr.find_all('td')])
