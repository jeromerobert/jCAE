/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2007, by EADS France
 */

//A workaround for charset encoding problems
%typemap(jni) jbyte[]  "jbyteArray"
%typemap(jtype) jbyte[] "byte[]"
%typemap(jstype) jbyte[] "byte[]"
%typemap(in) jbyte[]
{
	int length = JCALL1(GetArrayLength, jenv, $input);
	jbyte * name = new jbyte[length+1];
	JCALL4(GetByteArrayRegion, jenv, $input, 0, length, name); 
	name[length] = '\0';
	$1 = name;
}
%typemap(javain) jbyte[] "$javainput"


/**
 * XSControl_Reader
 */
 %{
#include <STEPControl_Reader.hxx>
#include <IGESControl_Reader.hxx>
#include <XSControl_WorkSession.hxx>
#include <XSControl_TransferReader.hxx>
#include <StepRepr_RepresentationItem.hxx>
#include <TCollection_HAsciiString.hxx>
#include <IGESData_IGESEntity.hxx>
#include <TransferBRep.hxx>
#include <Transfer_Binder.hxx>
#include <Transfer_TransientProcess.hxx>
#include <Interface_InterfaceModel.hxx>
#include <iostream>
 %}
class XSControl_Reader
{
	%javamethodmodifiers ReadFile(const Standard_CString filename)"
	/**
	 * @deprecated May segfault if path name include none-ASCII caracters. Use
	 * readFile(stringPath.getBytes()) instead.
	 */
	public";

	XSControl_Reader()=0;
	%rename(readFile) ReadFile;
	%rename(transferRoots) TransferRoots;
	%rename(clearShapes) ClearShapes;
	%rename(nbRootsForTransfer) NbRootsForTransfer;
	%rename(oneShape) OneShape;
	public:
	IFSelect_ReturnStatus ReadFile(const Standard_CString filename);
	//IFSelect_ReturnStatus ReadFile(jbyte filename[]);
	Standard_Integer TransferRoots() ;
	void ClearShapes();
	Standard_Integer NbRootsForTransfer();
	TopoDS_Shape OneShape() const;
};

%extend XSControl_Reader
{
	//A workaround for charset encoding problems
	IFSelect_ReturnStatus readFile(jbyte filename[])
	{
		return self->ReadFile((char*)filename);
	}
};

class STEPControl_Reader: public XSControl_Reader
{
	public:
	STEPControl_Reader();
};

%extend STEPControl_Reader
{
//dirty quick implementation of label step reading
//find how to generalize this to IGES
	char * getLabel(TopoDS_Shape * theShape)
	{
		const Handle(XSControl_WorkSession)& theSession = self->WS();
		const Handle(XSControl_TransferReader)& aReader = theSession->TransferReader();
		Handle(Standard_Transient) anEntity = aReader->EntityFromShapeResult(*theShape, 1);
		if (anEntity.IsNull()) {
			// as just mapped
			anEntity = aReader->EntityFromShapeResult (*theShape,-1);
		}

		if (anEntity.IsNull()) {
			// as anything
			anEntity = aReader->EntityFromShapeResult (*theShape,4);
		}

		if (anEntity.IsNull()) {
			cout<<"Warning: XSInterface_STEPReader::ReadAttributes() entity not found"<<endl;
			return NULL;
		}
		else 
		{
			Handle(StepRepr_RepresentationItem) aReprItem;
			aReprItem = Handle(StepRepr_RepresentationItem)::DownCast(anEntity);

			if (aReprItem.IsNull()) {
				cout<<"Error: STEPReader::ReadAttributes(): StepRepr_RepresentationItem Is NULL"<<endl;
				return NULL;
			}
			else 
				return aReprItem->Name()->ToCString();
		}
	}
};

class IGESControl_Reader: public XSControl_Reader
{
	public:
	IGESControl_Reader();
};

%extend IGESControl_Reader 
{
	//dirty quick implementation of label iges reading 
	const char * getLabel(TopoDS_Shape theShape) 
	{
		const Handle(XSControl_WorkSession)& theSession = self->WS();
		const Handle(Interface_InterfaceModel)& theModel = theSession->Model();
		const Handle(XSControl_TransferReader)& aReader = theSession->TransferReader();
		const Handle(Transfer_TransientProcess)& tp = aReader->TransientProcess();
		Standard_Integer nb = theModel->NbEntities(); 
		for(Standard_Integer i=1; i<=nb; i++) 
		{
			Handle(IGESData_IGESEntity) ent = Handle(IGESData_IGESEntity)::DownCast(theModel->Value(i));

			if (ent.IsNull())
				continue;

			Handle(Transfer_Binder) binder = tp->Find(ent); 

			if (binder.IsNull())
				continue;
			TopoDS_Shape oneShape = TransferBRep::ShapeResult(binder);
			if (oneShape.IsNull())
				continue;
			if (oneShape.IsEqual(theShape))
			{
				if (ent->HasName())
					return ent->NameValue()->String().ToCString();
				else
					return NULL; 
			}
		}
		return NULL; 
	}

	//get shape for label 
	TopoDS_Shape getShape(char* shapeName)
	{
		const TCollection_AsciiString ascShapeName(shapeName);
		const Handle(XSControl_WorkSession)& theSession = self->WS();
		const Handle(Interface_InterfaceModel)& theModel = theSession->Model();
		const Handle(XSControl_TransferReader)& aReader = theSession->TransferReader();
		const Handle(Transfer_TransientProcess)& tp = aReader->TransientProcess();
		Standard_Integer nb = theModel->NbEntities();
		TopoDS_Shape retShape; 
		for(Standard_Integer i=1; i<=nb; i++)
		{
			Handle(IGESData_IGESEntity) ent = Handle(IGESData_IGESEntity)::DownCast(theModel->Value(i));

			if (ent.IsNull())
				continue;
			Handle(Transfer_Binder) binder = tp->Find(ent);

			if (binder.IsNull())
				continue;
			TopoDS_Shape oneShape = TransferBRep::ShapeResult(binder);

			if (oneShape.IsNull())
				continue;

			if (ent->HasName() && ent->NameValue()->String().IsEqual(ascShapeName))
				retShape = oneShape;
		}
		return retShape; 
	}

	//dump all labels 
	void dumpLabels()
	{
		const Handle(XSControl_WorkSession)& theSession = self->WS();
		const Handle(Interface_InterfaceModel)& theModel = theSession->Model();
		Standard_Integer nb = theModel->NbEntities();
		for(Standard_Integer i=1; i<=nb; i++)
		{
			Handle(IGESData_IGESEntity) ent = Handle(IGESData_IGESEntity)::DownCast(theModel->Value(i));
			if (ent.IsNull()) continue;
			if (ent->HasName())
			{
				std::cout << ent->NameValue()->String().ToCString() << std::endl;
			}
		}
	}
};
