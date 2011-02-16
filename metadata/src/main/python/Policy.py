try:
    from lxml import etree
except ImportError:
    try:
        # Python 2.5
        import xml.etree.cElementTree as etree
    except ImportError:
        try:
            # Python 2.5
            import xml.etree.ElementTree as etree
        except ImportError:
            try:
                # normal cElementTree install
                import cElementTree as etree
            except ImportError:
                try:
                    # normal ElementTree install
                    import elementtree.ElementTree as etree
                except ImportError:
                    raise Exception("Failed to import ElementTree from any known place")

import ConfigParser


# keep: inputs: element tree
#	retrieve email endorser and checksum value from the elemet tree
#	return true email endorser listed in WhiteListEndorsers and checksum value is not listed in BlackListChecksums

def keep(xmltree):
    xpathPrefix = './/{http://mp.stratuslab.eu/slreq#}endorser/{http://mp.stratuslab.eu/slreq#}'
    emailendorser = xmltree.findtext(xpathPrefix + 'email')
    checksumimage = xmltree.findtext(xpathPrefix + 'value')
    if (emailendorser in WhiteListEndorsers) and (checksumimage not in BlackListChecksums):
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
    return xmltrees

# init : input : site config file policy
#	 read config file policy, constructs:  WhiteListEndorsers, WhiteListEndorsers and BlackListChecksums

def init(configfile):
    config = ConfigParser.ConfigParser()
    config.read(configfile)
    for i,j in config.items('whitelistendorsers'):
        WhiteListEndorsers.append(j)
    for i,j in config.items('blacklistendorsers'):
        WhiteListEndorsers.append(j)
    for i,j in config.items('blacklistchecksums'):
        BlackListChecksums.append(j)	


