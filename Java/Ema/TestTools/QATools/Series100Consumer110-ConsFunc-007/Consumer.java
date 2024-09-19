///*|----------------------------------------------------------------------------------------------------
// *|            This source code is provided under the Apache 2.0 license
// *|  and is provided AS IS with no warranty or guarantee of fit for purpose.
// *|                See the project's LICENSE.md for details.
// *|           Copyright (C) 2019 LSEG. All rights reserved.     
///*|----------------------------------------------------------------------------------------------------

//APIQA this file is QATools standalone. See qa_readme.txt for details about this tool.

package com.refinitiv.ema.examples.training.consumer.series100.ex110_MP_FileCfg;

import com.refinitiv.ema.access.Msg;
import com.refinitiv.ema.access.AckMsg;
import com.refinitiv.ema.access.GenericMsg;
import com.refinitiv.ema.access.OmmArray;
import com.refinitiv.ema.access.RefreshMsg;
import com.refinitiv.ema.access.StatusMsg;
import com.refinitiv.ema.access.UpdateMsg;
import com.refinitiv.ema.access.Data;
import com.refinitiv.ema.access.DataType;
import com.refinitiv.ema.access.DataType.DataTypes;
import com.refinitiv.ema.access.EmaFactory;
import com.refinitiv.ema.access.FieldEntry;
import com.refinitiv.ema.access.FieldList;
import com.refinitiv.ema.access.OmmConsumer;
import com.refinitiv.ema.access.OmmConsumerClient;
import com.refinitiv.ema.access.OmmConsumerEvent;
import com.refinitiv.ema.access.OmmException;
import com.refinitiv.ema.access.ElementList;
import com.refinitiv.ema.rdm.EmaRdm;

class AppClient implements OmmConsumerClient
{
    public void onRefreshMsg(RefreshMsg refreshMsg, OmmConsumerEvent event)
    {
        System.out.println("Item Name: " + (refreshMsg.hasName() ? refreshMsg.name() : "<not set>"));
        System.out.println("Service Name: " + (refreshMsg.hasServiceName() ? refreshMsg.serviceName() : "<not set>"));

        System.out.println("Item State: " + refreshMsg.state());
        System.out.println();
    }

    public void onUpdateMsg(UpdateMsg updateMsg, OmmConsumerEvent event)
    {
    }

    public void onStatusMsg(StatusMsg statusMsg, OmmConsumerEvent event)
    {
        System.out.println("Item Name: " + (statusMsg.hasName() ? statusMsg.name() : "<not set>"));
        System.out.println("Service Name: " + (statusMsg.hasServiceName() ? statusMsg.serviceName() : "<not set>"));

        if (statusMsg.hasState())
            System.out.println("Item State: " + statusMsg.state());

        System.out.println();
    }

    public void onGenericMsg(GenericMsg genericMsg, OmmConsumerEvent consumerEvent)
    {
    }

    public void onAckMsg(AckMsg ackMsg, OmmConsumerEvent consumerEvent)
    {
    }

    public void onAllMsg(Msg msg, OmmConsumerEvent consumerEvent)
    {
    }

    void decode(FieldList fieldList)
    {
        for (FieldEntry fieldEntry : fieldList)
        {
            System.out.print("Fid: " + fieldEntry.fieldId() + " Name = " + fieldEntry.name() + " DataType: " + DataType.asString(fieldEntry.load().dataType()) + " Value: ");

            if (Data.DataCode.BLANK == fieldEntry.code())
                System.out.println(" blank");
            else
                switch (fieldEntry.loadType())
                {
                    case DataTypes.REAL:
                        System.out.println(fieldEntry.real().asDouble());
                        break;
                    case DataTypes.DATE:
                        System.out.println(fieldEntry.date().day() + " / " + fieldEntry.date().month() + " / " + fieldEntry.date().year());
                        break;
                    case DataTypes.TIME:
                        System.out.println(fieldEntry.time().hour() + ":" + fieldEntry.time().minute() + ":" + fieldEntry.time().second() + ":" + fieldEntry.time().millisecond());
                        break;
                    case DataTypes.INT:
                        System.out.println(fieldEntry.intValue());
                        break;
                    case DataTypes.UINT:
                        System.out.println(fieldEntry.uintValue());
                        break;
                    case DataTypes.ASCII:
                        System.out.println(fieldEntry.ascii());
                        break;
                    case DataTypes.ENUM:
                        System.out.println(fieldEntry.enumValue());
                        break;
                    case DataTypes.RMTES :
                        System.out.println(fieldEntry.rmtes());
                        break;
                    case DataTypes.ERROR:
                        System.out.println("(" + fieldEntry.error().errorCodeAsString() + ")");
                        break;
                    default:
                        System.out.println();
                        break;
                }
        }
    }
}

public class Consumer
{
    public static void main(String[] args)
    {
        OmmConsumer consumer = null;
        try
        {
            /*need to run rsslProvider app with only send refresh without updates
             * turn on logging.properties as FINEST level
             * In ItemCallbackClient.java, sets CONSUMER_STARTING_STREAM_ID = 2147483636
             * run 4 minutes
            * validate the following:
            * capture "Reach max number available for next stream id, will wrap around"
            * capture 4 status msg with text "Stream closed for batch"
            * capture 2 msg with text "treamId 0 to item map" for batch item which was created by EMA internal for splitting
            * capture "Added Item 1 of StreamId 2147483638 to item map"
            * capture "Removed Item 1 of StreamId 2147483638 from item map"
            * capture more than 2 times for "2147483638"
            * capture "
            * Item Name: TRI.N_14
            Service Name: DIRECT_FEED
            Item State: Open / Ok / None / 'Item Refresh Completed'
            */
            AppClient appClient = new AppClient();

            consumer = EmaFactory.createOmmConsumer(EmaFactory.createOmmConsumerConfig().username("user"));
            int requiredNum = 14;
            int numItems = 0;
            boolean snapshot = false;
            ElementList batch = EmaFactory.createElementList();
            OmmArray array = EmaFactory.createOmmArray();
            while (numItems < requiredNum)
            {
                array.clear();
                batch.clear();
                if (snapshot)
                    snapshot = false;
                else
                    snapshot = true;

                if (snapshot)
                {
                    array.add(EmaFactory.createOmmArrayEntry().ascii("TRI.N_" + ++numItems));
                    array.add(EmaFactory.createOmmArrayEntry().ascii("TRI.N_" + ++numItems));
                    array.add(EmaFactory.createOmmArrayEntry().ascii("TRI.N_" + ++numItems));
                    array.add(EmaFactory.createOmmArrayEntry().ascii("TRI.N_" + ++numItems));
                    array.add(EmaFactory.createOmmArrayEntry().ascii("TRI.N_" + ++numItems));
                    batch.add(EmaFactory.createElementEntry().array(EmaRdm.ENAME_BATCH_ITEM_LIST, array));
                    consumer.registerClient(EmaFactory.createReqMsg().serviceName("DIRECT_FEED").interestAfterRefresh(false).payload(batch), appClient);
                }
                else
                {
                    array.add(EmaFactory.createOmmArrayEntry().ascii("TRI.N_" + ++numItems));
                    array.add(EmaFactory.createOmmArrayEntry().ascii("TRI.N_" + ++numItems));
                    batch.add(EmaFactory.createElementEntry().array(EmaRdm.ENAME_BATCH_ITEM_LIST, array));
                    consumer.registerClient(EmaFactory.createReqMsg().serviceName("DIRECT_FEED").interestAfterRefresh(true).payload(batch), appClient);
                }

                Thread.sleep(2000);
            }

            Thread.sleep(5000); // API calls onRefreshMsg(), onUpdateMsg() and
                                // onStatusMsg()
        }
        catch (InterruptedException | OmmException excp)
        {
            System.out.println(excp.getMessage());
        }
        finally
        {
            if (consumer != null)
                consumer.uninitialize();
        }
    }
}
//END APIQA
