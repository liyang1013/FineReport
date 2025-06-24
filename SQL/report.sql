create table deviceinfo
(
    deviceId   varchar(16)  not null comment '安卓设备唯一ID'
        primary key,
    remark     varchar(50)  null comment '备注',
    ipAddress  varchar(15)  null comment 'IP',
    url        varchar(255) null comment '打开网址',
    lastSeen   datetime     null,
    position   varchar(50)  null comment '位置',
    department varchar(50)  null comment '部门',
    name       varchar(50)  null,
    type       varchar(50)  null comment '类型'
);