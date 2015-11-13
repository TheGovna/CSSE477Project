import httplib
import random
import json
import time

def simulateTraffic():
	requestTypes = ['GET', 'PUT', 'POST', 'DELETE'];
	server = httplib.HTTPConnection('localhost', 8023)
	server.connect()
	while(1):
		time.sleep(0.1)
		randomRequest = random.choice(requestTypes)
		
		if randomRequest == 'GET':
			getOrModify = random.randint(0, 1);
			
			if getOrModify == 0:
				# GET all books
				print("Attempting GET ALL")
				server.request('GET', '/Plugin1/books')
			else:
				# GET specific book
				print("Attempting GET MODIFY")
				randomBook = getRandomBook()
				server.request('GET', '/Plugin1/book/' + randomBook['author'] + "/" + randomBook['title'])
					
		elif randomRequest == 'PUT':
			randomBook = getRandomBook()
			modifyAuthorOrTitle = random.randint(0, 1);
			print("Attempting PUT")

			if modifyAuthorOrTitle == 0:
				# Modify author
				randomAuthorNumber = random.randint(1, 100)
				server.request('PUT', '/Plugin1/book/Author' + str(randomAuthorNumber) + "/" + randomBook['title'])
			else:
				# Modify title
				randomTitleNumber = random.randint(1, 100)
				server.request('PUT', '/Plugin1/book/' + randomBook['author'] + "/Title" + str(randomTitleNumber))

		elif randomRequest == 'POST':
			print("Attempting POST")
			randomAuthorNumber = random.randint(1, 100)
			randomTitleNumber = random.randint(1, 100)
			server.request('POST', '/Plugin1/book/Author' + str(randomAuthorNumber) + "/Title" + str(randomTitleNumber))
		else:
			# DELETE request
			print("Attempting DELETE")
			randomBook = getRandomBook()
			server.request('DELETE', '/Plugin1/book/' + randomBook['author'] + "/" + randomBook['title'])

		response = server.getresponse()
		print randomRequest + " " + str(response.status)

def getRandomBook():
	with open('../books.json') as data_file:
		booksJson = json.load(data_file)
		randomBook = random.choice(booksJson)
		return randomBook

def main():
	simulateTraffic()

if __name__ == '__main__':
	main()