from xml.etree.ElementTree import ElementTree


# keep: inputs: element tree
	retrieve email endorser and checksum value from the elemet tree
	return true email endorser listed in WhiteListEndorsers and checksum value is not listed in BlackListChecksums

def keep(xmltree):
	emailendorser = xmltree.findtext(".//{http://mp.stratuslab.eu/slreq#}endorser/{http://mp.stratuslab.eu/slreq#}email")
	checksumimage = xmltree.findtext(".//{http://mp.stratuslab.eu/slreq#}checksum/{http://mp.stratuslab.eu/slreq#}value")
        if emailendorser in WhiteListEndorsers and checksumimage not in BlackListChecksums:
           return True
	else:
	   return False

#filter: inputs: metadatas list as elementt ree.
#	 remove unwanted element tree from metadatas list
#	 return metadatas list  

def filter(xmltrees):
	for xmltree in xmltrees:
	  if keep(xmltree) == False:
		xmltrees.remove(xmltree)
		print "xmltree removed=",xmltree
        return xmltrees



# init : input : site config file policy
	 read config file policy, constructs:  WhiteListEndorsers, WhiteListEndorsers and BlackListChecksums

def init(configfile):
    import ConfigParser
    config = ConfigParser.ConfigParser()
    config.read(configfile)
    for i,j in config.items('whitelistendorsers'):
	WhiteListEndorsers.append(j)
    for i,j in config.items('blacklistendorsers'):
        WhiteListEndorsers.append(j)
    for i,j in config.items('blacklistchecksums'):
        BlackListChecksums.append(j)	



#Testing filter, init, keep plugins:

if __name__ == "__main__":
    metadata = ["valid-full.xml", "hackers-full.xml"]
    
    WhiteListEndorsers=[]
    BlackListEndorsers=[]
    BlackListChecksums=[]
    
    xmltree = ElementTree()
    xmltree2 = ElementTree()
    xmltree.parse("valid-full.xml")
    xmltree2.parse("hackers-full.xml")
    
    xmltrees = [xmltree, xmltree2]
    
    init('policy.cfg') 
    
    print 'len(xmltrees)=',len(xmltrees)
    filter(xmltrees)
    print 'len(xmltrees)=',len(xmltrees)
