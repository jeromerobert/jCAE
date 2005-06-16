function getTextForElement(obj) 
{
	var str=""
	for (var i=0;i < obj.childNodes.length;i++)
	{
		if (obj.childNodes[i].nodeType==1)
			str+=getTextForElement(obj.childNodes[i])
		else if (obj.childNodes[i].nodeType==3)
			str = obj.childNodes[i].data
	}
	return str
}

function doLoad()
{
	var obj = document.getElementsByTagName("*")
	var ul=document.createElement("ul");
	var tagList = "H2;H3;"
	for (var i=0;i < obj.length;i++)
	{
		if (tagList.indexOf(obj[i].tagName+";")>=0)
		{
			var textNode=document.createTextNode(getTextForElement(obj[i]))
			var a=document.createElement("a");
			a.setAttribute("name","tabcontent"+i);
			obj[i].insertBefore(a, obj[i].firstChild);
			var li=document.createElement("li");
			a=document.createElement("a");
			a.appendChild(textNode);
			a.setAttribute("href","#tabcontent"+i);

			if(obj[i].tagName=="H2")
			{
				li.appendChild(a);
			}
			else if(obj[i].tagName=="H3")
			{
				var ul2=document.createElement("ul");
				var li2=document.createElement("li");
				li2.appendChild(a);
				ul2.appendChild(li2);
				li=ul2;
			}

			ul.appendChild(li);
		}
	}
	
	var montitre = document.createElement("h1");
	montitre.appendChild(document.createTextNode(document.title));
	var sommaire = document.createElement("h2");
	sommaire.appendChild(document.createTextNode("Sommaire"));

	var body = document.getElementsByTagName("body")[0];
	var bc=body.childNodes[0];
	body.insertBefore(montitre, bc);
	body.insertBefore(sommaire, bc);
	body.insertBefore(ul, bc);
}

function ie_getElementsByTagName(str)
{
	if (str=="*")
		return document.all
	else
		return document.all.tags(str)
}

if (document.all)
	document.getElementsByTagName = ie_getElementsByTagName

window.onload =doLoad

