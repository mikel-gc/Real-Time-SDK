///*|-----------------------------------------------------------------------------
// *|            This source code is provided under the Apache 2.0 license
// *|  and is provided AS IS with no warranty or guarantee of fit for purpose.
// *|                See the project's LICENSE.md for details.
// *|           Copyright (C) 2019 LSEG. All rights reserved.     
///*|-----------------------------------------------------------------------------

//APIQA this file is QATools standalone. See qa_readme.txt for details about this tool.

package com.refinitiv.ema.examples.training.iprovider.series100.ex100_MP_Streaming;

import com.refinitiv.ema.access.EmaFactory;
import com.refinitiv.ema.access.FieldList;
import com.refinitiv.ema.access.GenericMsg;
import com.refinitiv.ema.access.Msg;
import com.refinitiv.ema.access.OmmException;
import com.refinitiv.ema.access.OmmIProviderConfig;
import com.refinitiv.ema.access.OmmProvider;
import com.refinitiv.ema.access.OmmProviderClient;
import com.refinitiv.ema.access.OmmProviderEvent;
import com.refinitiv.ema.access.OmmReal;
import com.refinitiv.ema.access.OmmState;
import com.refinitiv.ema.access.PostMsg;
import com.refinitiv.ema.access.RefreshMsg;
import com.refinitiv.ema.access.ReqMsg;
import com.refinitiv.ema.access.StatusMsg;
import com.refinitiv.ema.rdm.EmaRdm;

class AppClient implements OmmProviderClient
{
    public long itemHandle = 0;

    public void onReqMsg(ReqMsg reqMsg, OmmProviderEvent event)
    {
        switch (reqMsg.domainType())
        {
            case EmaRdm.MMT_LOGIN:
                processLoginRequest(reqMsg, event);
                break;
            case EmaRdm.MMT_MARKET_PRICE:
                processMarketPriceRequest(reqMsg, event);
                break;
            default:
                processInvalidItemRequest(reqMsg, event);
                break;
        }
    }

    public void onRefreshMsg(RefreshMsg refreshMsg, OmmProviderEvent event)
    {
    }

    public void onStatusMsg(StatusMsg statusMsg, OmmProviderEvent event)
    {
    }

    public void onGenericMsg(GenericMsg genericMsg, OmmProviderEvent event)
    {
    }

    public void onPostMsg(PostMsg postMsg, OmmProviderEvent event)
    {
    }

    public void onReissue(ReqMsg reqMsg, OmmProviderEvent event)
    {
    }

    public void onClose(ReqMsg reqMsg, OmmProviderEvent event)
    {
    }

    public void onAllMsg(Msg msg, OmmProviderEvent event)
    {
    }

    void processLoginRequest(ReqMsg reqMsg, OmmProviderEvent event)
    {
        event.provider().submit(EmaFactory.createRefreshMsg().domainType(EmaRdm.MMT_LOGIN).name(reqMsg.name()).nameType(EmaRdm.USER_NAME).complete(true).solicited(true)
                                        .state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Login accepted"), event.handle());
    }

    void processMarketPriceRequest(ReqMsg reqMsg, OmmProviderEvent event)
    {
        if (itemHandle != 0)
        {
            processInvalidItemRequest(reqMsg, event);
            return;
        }

        FieldList fieldList = EmaFactory.createFieldList();
        fieldList.add(EmaFactory.createFieldEntry().enumValue(4, 3));
        fieldList.add(EmaFactory.createFieldEntry().enumValue(54, 235));
        fieldList.add(EmaFactory.createFieldEntry().enumValue(103, 8));
        fieldList.add(EmaFactory.createFieldEntry().enumValue(13416, 185));
        fieldList.add(EmaFactory.createFieldEntry().enumValue(1709, 536));
        fieldList.add(EmaFactory.createFieldEntry().enumValue(54, 3));

        event.provider().submit(EmaFactory.createRefreshMsg().name(reqMsg.name()).serviceId(reqMsg.serviceId()).solicited(true)
                                        .state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Refresh Completed").payload(fieldList).complete(true), event.handle());

        itemHandle = event.handle();
    }

    void processInvalidItemRequest(ReqMsg reqMsg, OmmProviderEvent event)
    {
        event.provider().submit(EmaFactory.createStatusMsg().name(reqMsg.name()).serviceName(reqMsg.serviceName())
                                        .state(OmmState.StreamState.CLOSED, OmmState.DataState.SUSPECT, OmmState.StatusCode.NOT_FOUND, "Item not found"), event.handle());
    }
}

public class IProvider
{
    public static void main(String[] args)
    {
        OmmProvider provider = null;
        try
        {
            AppClient appClient = new AppClient();
            FieldList fieldList = EmaFactory.createFieldList();

            OmmIProviderConfig config = EmaFactory.createOmmIProviderConfig();

            provider = EmaFactory.createOmmProvider(config.port("14002"), appClient);

            while (appClient.itemHandle == 0)
                Thread.sleep(1000);

            for (int i = 0; i < 60; i++)
            {
                fieldList.clear();
                fieldList.add(EmaFactory.createFieldEntry().enumValue(4, 77));
                fieldList.add(EmaFactory.createFieldEntry().enumValue(54, 236));
                fieldList.add(EmaFactory.createFieldEntry().enumValue(103, 16));
                fieldList.add(EmaFactory.createFieldEntry().enumValue(13416, 3000));
                fieldList.add(EmaFactory.createFieldEntry().enumValue(1709, 1164));
                fieldList.add(EmaFactory.createFieldEntry().enumValue(54, 68));

                provider.submit(EmaFactory.createUpdateMsg().payload(fieldList), appClient.itemHandle);

                Thread.sleep(1000);
            }
        }
        catch (InterruptedException | OmmException excp)
        {
            System.out.println(excp.getMessage());
        }
        finally
        {
            if (provider != null)
                provider.uninitialize();
        }
    }
}
//END APIQA
