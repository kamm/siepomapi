@Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.14') 
import org.cyberneko.html.parsers.SAXParser 
import groovy.util.XmlSlurper
import groovy.json.JsonOutput
import java.text.Normalizer
@Grab(group='org.apache.commons', module='commons-lang3', version='3.0')
import org.apache.commons.lang3.StringUtils
import groovy.json.*

class HDD {
    static save(Object content, String filePath) {
        new File(filePath).write(new JsonBuilder(content).toString())
    }

    static Object load(String filePath) {
        def file = new File(filePath)
        if(file.exists()){
            return new JsonSlurper().parseText(file.text)
        }else{
            return [];
        }
    }
}

class SiePomaga{
    static parsePayments(String boxName){
        def parser = new SAXParser()
        def host = "https://www.siepomaga.pl/${boxName}"
    
        //def base_run = new XmlSlurper(parser).parseText(host.toURL().getText(requestProperties: ['User-Agent': 'Non empty']))
        //def 
        //def total = base_run.'**'.find{it.@class.text().endsWith(' can')}.SPAN.text().replaceAll("[^0-9.]","").trim()
        //println total
        
        def paymentList = HDD.load("${boxName}.json") 
        
        println paymentList.size()
    
    
    
        println "Jedziemy..."
        def u=1;
        for(int i=1;i<=u;i++){
          println "Scrapping page ${i} of ${u}"
          def html = new XmlSlurper(parser).parseText("${host}?payment_page=${i}".toURL().getText(requestProperties: ['User-Agent': 'Non empty']))
          if(u==1){
            u = Integer.parseInt(html.'**'.find{it.@class=='last'}.A.@href.text().replaceAll(".*payment_page=",""))
          }
          
          def list = html.'**'.find{it.@class == 'payments'}.UL[0].LI;
          
          for(def it: list){
             def comment=it.'**'.find{it.@class=='comment'}.text().trim()
             
             def rawHashtag = StringUtils.stripAccents(comment).toLowerCase().replace("ł","l")
             
             def finder = (rawHashtag =~ /#([^\s]+)/)
             def hashtag = ""
             if(finder.count > 0){
                 hashtag = finder[0][1]
                 hashtag = hashtag.replaceAll("[^a-z0-9]","")
                 //hashtag = "#${hashtag}"
             }
             
             
             def user=it.'**'.find{it.name()=='H5'}.text().trim()
             def amountStr = it.'**'.find{it.@class=='amount'}.text().trim()
             def amount = amountStr.replaceAll("[^0-9,]","").replace(",",".")
             int amountInt = 0
             if(!amount.isEmpty()){
                 amountInt = (int) (100 * Double.parseDouble(amount))
             }
             def date=it.'**'.find{it.@class=='date'}.text().trim()
             
             def obj = [user: user, amount: amountInt, amountStr: amountStr, hashtag: hashtag, comment: comment, date: date]
             if(paymentList.contains(obj)){
                HDD.save(paymentList, "${boxName}.json")
                return;
             }else{
                paymentList << obj
             }
                  
          }
        }
        
        HDD.save(paymentList, "${boxName}.json")
    }
    
    static stats(String boxName){
        def list = HDD.load("${boxName}.json")
        def hashtagMap = [:]
        for(payment in list){
            if(! payment.hashtag.isEmpty()){
                
                if(hashtagMap[payment.hashtag] == null){
                    hashtagMap[payment.hashtag] = 0
                }
                try{
                    hashtagMap[payment.hashtag] += payment.amount
                }catch(e){
                    println "Bad value ${payment.amount}"
                }
                
            }
        }
        HDD.save(hashtagMap,"${boxName}-stats.json")
        println new JsonBuilder(hashtagMap).toPrettyString()
    }
}

if(args.length != 1){
    println "Usage 'groovy siepomapi.groovy <name>'"
    System.exit(1)
}
def boxName = args[0]
SiePomaga.parsePayments(boxName)
SiePomaga.stats(boxName)








