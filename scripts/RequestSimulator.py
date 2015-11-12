import httplib
import random

def simulateTraffic():
	requestTypes = ['GET', 'PUT', 'POST', 'DELETE'];
	server = httplib.HTTPConnection('localhost', 8023)
	server.connect()
	while(1):
		# TODO: random pause
		randomRequest = random.choice(requestTypes)
		
		if randomRequest == 'GET':
			print ""
		elif randomRequest == 'PUT':
			print ""
		elif randomRequest == 'POST':
			randomAuthorNumber = random.randint(1, 100)
			randomTitleNumber = random.randint(1, 100)
			server.request('POST', '/Plugin1/book/Author' + str(randomAuthorNumber) + "/Title" + str(randomTitleNumber))
		else:
			print ""

		response = server.getresponse()
		if response.status == httplib.OK:
			print "got OK"


def main():
	simulateTraffic()

if __name__ == '__main__':
	main()