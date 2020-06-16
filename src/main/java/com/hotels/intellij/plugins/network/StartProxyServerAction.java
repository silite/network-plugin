/*
 * Copyright 2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.intellij.plugins.network;

import com.hotels.intellij.plugins.network.converter.DefaultByteBufToStringConverter;
import com.hotels.intellij.plugins.network.converter.FullHttpResponseToResponseContentConverter;
import com.hotels.intellij.plugins.network.converter.HeaderToTextConverter;
import com.hotels.intellij.plugins.network.converter.LZ4ByteBufToStringConverter;
import com.hotels.intellij.plugins.network.converter.NettyObjectsToRequestResponseConverter;
import com.hotels.intellij.plugins.network.converter.RequestToCurlConverter;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

/**
 * Button to start the proxy server.
 */
public class StartProxyServerAction extends AnAction {

    private NetworkTableModel tableModel;

    public StartProxyServerAction(NetworkTableModel tableModel) {
        super("Start Server", "", AllIcons.Actions.Execute);

        this.tableModel = tableModel;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        int httpPort = getProxyPort();
        notifyProxyStartup(httpPort);

        HttpProxyServer httpProxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(httpPort)
                .withFiltersSource(getProxyHttpFiltersSourceAdapter())
                .withMaxInitialLineLength(32768)
                .withTransparent(true)
                .start();

        ProxyServerService proxyServerService = event.getProject().getService(ProxyServerService.class);
        proxyServerService.setHttpProxyServer(httpProxyServer);
    }

    @Override
    public void update(AnActionEvent event) {
        ProxyServerService proxyServerService = event.getProject().getService(ProxyServerService.class);
        event.getPresentation().setEnabled(proxyServerService.getHttpProxyServer() == null);
    }

    private int getProxyPort() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        return propertiesComponent.getInt(Preferences.HTTP_PORT_KEY, Preferences.HTTP_PORT_DEFAULT);
    }

    private void notifyProxyStartup(int httpPort) {
        com.intellij.notification.Notification notification = new com.intellij.notification.Notification(NotificationConstants.NOTIFICATION_GROUP_ID,
                NotificationConstants.NOTIFICATION_TITLE,
                "Starting proxy server on port " + httpPort + ".",
                NotificationType.INFORMATION);

        Notifications.Bus.notify(notification);
    }

    private ProxyHttpFiltersSourceAdapter getProxyHttpFiltersSourceAdapter() {
        DefaultByteBufToStringConverter defaultByteBufToStringConverter = new DefaultByteBufToStringConverter();
        LZ4ByteBufToStringConverter lz4ByteBufToStringConverter = new LZ4ByteBufToStringConverter();
        HeaderToTextConverter headerToTextConverter = new HeaderToTextConverter();
        RequestToCurlConverter requestToCurlConverter = new RequestToCurlConverter();
        FullHttpResponseToResponseContentConverter fullHttpResponseToResponseContentConverter = new FullHttpResponseToResponseContentConverter(defaultByteBufToStringConverter, lz4ByteBufToStringConverter);
        NettyObjectsToRequestResponseConverter nettyObjectsToRequestResponseConverter = new NettyObjectsToRequestResponseConverter(defaultByteBufToStringConverter, fullHttpResponseToResponseContentConverter, headerToTextConverter, requestToCurlConverter);

        NetworkListener networkListener = new NetworkListener(tableModel, nettyObjectsToRequestResponseConverter);
        return new ProxyHttpFiltersSourceAdapter(networkListener);
    }
}
