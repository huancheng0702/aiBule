//
//  DemoViewController.m
//  sdkdemo
//
//  Created by coolwear on 2022/9/20.
//

#import "FuncListViewController.h"
#import "ListSwtCell.h"
#import "DialViewController.h"
#import "AppViewController.h"
#import "AlarmViewController.h"
#import "FuncModel.h"
#import "RealTestViewController.h"

@interface FuncListViewController ()<UITableViewDataSource, UITableViewDelegate>
@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) UIView *resultPanel;
@property (nonatomic, strong) UILabel *connectionLabel;
@property (nonatomic, strong) UILabel *lastCommandLabel;
@property (nonatomic, strong) UITextView *resultTextView;
@property (nonatomic, strong) NSMutableArray<NSString *> *resultLogs;
@property (nonatomic, strong) NSArray *data;

@property (nonatomic, assign) BOOL ppGswitch;
@property (nonatomic, assign) BOOL gsensorswitch;


@end

@implementation FuncListViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor colorWithRed:0.95 green:0.97 blue:0.99 alpha:1.0];
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(dataReceiveFinish:) name:@"DataReceivceFinish" object:nil];
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(originalDataLog:) name:@"CEOriginalDataLog" object:nil];
    [self initData];
    [self initUI];
    [self appendLog:@"Waiting for commands or device response data" direction:@"INFO"];
    
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    
    CGFloat safeTop = 0;
    CGFloat safeBottom = 0;
    if (@available(iOS 11.0, *)) {
        safeTop = self.view.safeAreaInsets.top;
        safeBottom = self.view.safeAreaInsets.bottom;
    }
    
    CGFloat width = self.view.bounds.size.width;
    CGFloat height = self.view.bounds.size.height;
    CGFloat panelHeight = MIN(280, MAX(220, height * 0.36));
    self.resultPanel.frame = CGRectMake(12, height - safeBottom - panelHeight - 12, width - 24, panelHeight);
    self.tableView.frame = CGRectMake(0, safeTop, width, CGRectGetMinY(self.resultPanel.frame) - safeTop - 10);
    
    CGFloat panelWidth = self.resultPanel.bounds.size.width;
    self.connectionLabel.frame = CGRectMake(14, 12, 86, 26);
    self.lastCommandLabel.frame = CGRectMake(CGRectGetMaxX(self.connectionLabel.frame) + 8, 12, panelWidth - CGRectGetMaxX(self.connectionLabel.frame) - 22, 26);
    self.resultTextView.frame = CGRectMake(12, 48, panelWidth - 24, panelHeight - 60);
}

- (void)dealloc {
    [NSNotificationCenter.defaultCenter removeObserver:self];
}


- (void)initData{
        
    FuncModel *dial = [[FuncModel alloc] initWithTitle:@"Watch Face" type:0 page:@"DialViewController"];
    FuncModel *app = [[FuncModel alloc] initWithTitle:@"App Notifications" type:0 page:@"AppViewController"];
    FuncModel *alarm = [[FuncModel alloc] initWithTitle:@"Alarm" type:0 page:@"AlarmViewController"];
    FuncModel *sms = [[FuncModel alloc] initWithTitle:@"SMS Reminder" type:1];
    FuncModel *call = [[FuncModel alloc] initWithTitle:@"Call Reminder" type:1];
    FuncModel *longsit = [[FuncModel alloc] initWithTitle:@"Sedentary Reminder" type:1];
    FuncModel *goal = [[FuncModel alloc] initWithTitle:@"Goal Reminder" type:1];
    FuncModel *water = [[FuncModel alloc] initWithTitle:@"Drink Water Reminder" type:1];
    FuncModel *time = [[FuncModel alloc] initWithTitle:@"Time Format" type:1];
    FuncModel *date = [[FuncModel alloc] initWithTitle:@"Date Format" type:1];
    FuncModel *find = [[FuncModel alloc] initWithTitle:@"Find Band" type:2];
    FuncModel *disturb = [[FuncModel alloc] initWithTitle:@"Do Not Disturb" type:1];
    FuncModel *raise = [[FuncModel alloc] initWithTitle:@"Raise to Wake" type:1];
    FuncModel *autoHr = [[FuncModel alloc] initWithTitle:@"Auto Heart Rate" type:1];
    FuncModel *takePhoto = [[FuncModel alloc] initWithTitle:@"Remote Camera" type:1];
    FuncModel *realHeartPPG = [[FuncModel alloc] initWithTitle:@"Heart Rate PPG" type:1];
    FuncModel *realGensor = [[FuncModel alloc] initWithTitle:@"G-Sensor Data" type:1];

    FuncModel *ota = [[FuncModel alloc] initWithTitle:@"OTA" type:2];
    FuncModel *weather = [[FuncModel alloc] initWithTitle:@"Send Weather" type:2];
    FuncModel *contact = [[FuncModel alloc] initWithTitle:@"Contacts" type:0 page:@"ContactViewController"];
    FuncModel *realhr = [[FuncModel alloc] initWithTitle:@"Real-time Heart Rate" type:2];
    FuncModel *realo2 = [[FuncModel alloc] initWithTitle:@"Real-time Blood Pressure" type:2];
    FuncModel *realbp = [[FuncModel alloc] initWithTitle:@"Real-time Blood Oxygen" type:2];
    FuncModel *realHrv= [[FuncModel alloc] initWithTitle:@"Real-time HRV" type:2];

    self.data = @[dial, app, alarm, sms, call, longsit, goal, water, time, date, find, disturb, raise, autoHr, takePhoto,realHeartPPG,realGensor, ota, weather, contact, realhr, realo2, realbp,realHrv];
    
}

- (void)initUI{
    
    
    self.title = self.sp.name;
    
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Back" style:UIBarButtonItemStylePlain target:self action:@selector(backAction:)];
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Clear" style:UIBarButtonItemStylePlain target:self action:@selector(clearLogs)];
    
    self.tableView = [[UITableView alloc] initWithFrame:CGRectZero style:UITableViewStylePlain];
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.tableView.showsVerticalScrollIndicator = NO;
    self.tableView.rowHeight = 55;
    self.tableView.backgroundColor = UIColor.clearColor;
    self.tableView.separatorInset = UIEdgeInsetsMake(0, 16, 0, 16);
    [self.view addSubview:self.tableView];
    
    self.resultPanel = [[UIView alloc] initWithFrame:CGRectZero];
    self.resultPanel.backgroundColor = UIColor.whiteColor;
    self.resultPanel.layer.cornerRadius = 8;
    self.resultPanel.layer.borderWidth = 1;
    self.resultPanel.layer.borderColor = [UIColor colorWithRed:0.86 green:0.89 blue:0.93 alpha:1.0].CGColor;
    self.resultPanel.layer.shadowColor = UIColor.blackColor.CGColor;
    self.resultPanel.layer.shadowOpacity = 0.08;
    self.resultPanel.layer.shadowRadius = 12;
    self.resultPanel.layer.shadowOffset = CGSizeMake(0, 4);
    [self.view addSubview:self.resultPanel];
    
    self.connectionLabel = [[UILabel alloc] initWithFrame:CGRectZero];
    self.connectionLabel.text = @"Response";
    self.connectionLabel.textColor = UIColor.whiteColor;
    self.connectionLabel.backgroundColor = [UIColor colorWithRed:0.12 green:0.47 blue:0.85 alpha:1.0];
    self.connectionLabel.font = [UIFont systemFontOfSize:13 weight:UIFontWeightSemibold];
    self.connectionLabel.textAlignment = NSTextAlignmentCenter;
    self.connectionLabel.layer.cornerRadius = 5;
    self.connectionLabel.layer.masksToBounds = YES;
    [self.resultPanel addSubview:self.connectionLabel];
    
    self.lastCommandLabel = [[UILabel alloc] initWithFrame:CGRectZero];
    self.lastCommandLabel.text = @"Last command: None";
    self.lastCommandLabel.textColor = [UIColor colorWithRed:0.23 green:0.27 blue:0.33 alpha:1.0];
    self.lastCommandLabel.font = [UIFont systemFontOfSize:13 weight:UIFontWeightMedium];
    [self.resultPanel addSubview:self.lastCommandLabel];
    
    self.resultTextView = [[UITextView alloc] initWithFrame:CGRectZero];
    self.resultTextView.editable = NO;
    self.resultTextView.selectable = YES;
    self.resultTextView.backgroundColor = [UIColor colorWithRed:0.07 green:0.09 blue:0.12 alpha:1.0];
    self.resultTextView.textColor = [UIColor colorWithRed:0.86 green:0.93 blue:1.0 alpha:1.0];
    if (@available(iOS 13.0, *)) {
        self.resultTextView.font = [UIFont monospacedSystemFontOfSize:12 weight:UIFontWeightRegular];
    } else {
        self.resultTextView.font = [UIFont fontWithName:@"Menlo" size:12] ?: [UIFont systemFontOfSize:12];
    }
    self.resultTextView.layer.cornerRadius = 6;
    self.resultTextView.textContainerInset = UIEdgeInsetsMake(10, 10, 10, 10);
    [self.resultPanel addSubview:self.resultTextView];
    
}

- (void)dataReceiveFinish:(NSNotification *)noti{
    [self.tableView reloadData];
    NSString *message = [self prettyStringFromObject:noti.userInfo];
    [self appendLog:message.length > 0 ? message : @"Received device data" direction:@"RECV"];
}

- (void)originalDataLog:(NSNotification *)noti {
    NSString *direction = noti.userInfo[@"direction"] ?: @"RAW";
    NSString *data = noti.userInfo[@"data"] ?: @"";
    [self appendLog:data direction:direction];
}

- (void)backAction:(id)sender{
    [[CEProductK6 shareInstance] releaseBind];
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (NSMutableArray<NSString *> *)resultLogs {
    if (!_resultLogs) {
        _resultLogs = [NSMutableArray array];
    }
    return _resultLogs;
}

- (void)clearLogs {
    [self.resultLogs removeAllObjects];
    self.resultTextView.text = @"";
    self.lastCommandLabel.text = @"Last command: None";
}

- (void)appendLog:(NSString *)message direction:(NSString *)direction {
    if (message.length == 0) {
        return;
    }
    
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    formatter.dateFormat = @"HH:mm:ss";
    NSString *time = [formatter stringFromDate:NSDate.date];
    NSString *log = [NSString stringWithFormat:@"[%@] %@\n%@", time, direction, message];
    [self.resultLogs addObject:log];
    if (self.resultLogs.count > 40) {
        [self.resultLogs removeObjectAtIndex:0];
    }
    
    self.resultTextView.text = [self.resultLogs componentsJoinedByString:@"\n\n"];
    NSRange bottom = NSMakeRange(MAX((NSInteger)self.resultTextView.text.length - 1, 0), 1);
    [self.resultTextView scrollRangeToVisible:bottom];
}

- (void)recordCommandWithTitle:(NSString *)title {
    NSString *command = [NSString stringWithFormat:@"Last command: %@", title ?: @"Unknown"];
    self.lastCommandLabel.text = command;
    [self appendLog:title ?: @"Unknown command" direction:@"SEND"];
}

- (NSString *)prettyStringFromObject:(id)object {
    if (!object) {
        return @"";
    }
    
    if ([NSJSONSerialization isValidJSONObject:object]) {
        NSError *error = nil;
        NSData *data = [NSJSONSerialization dataWithJSONObject:object options:NSJSONWritingPrettyPrinted error:&error];
        if (!error && data) {
            return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        }
    }
    
    return [object description];
}


- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section{
    return self.data.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath{
    
    FuncModel *model = self.data[indexPath.row];
    
    if (model.type == 1) {
        ListSwtCell* cell = [[ListSwtCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:nil];
        cell.selectionStyle = UITableViewCellSelectionStyleNone;
        cell.textLabel.text = model.title;
        cell.swt.tag = indexPath.row;
        [cell.swt addTarget:self action:@selector(swtAction:) forControlEvents:UIControlEventTouchUpInside];
        [self updateCell:cell atIndexPath:indexPath];
        return cell;
    }
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"cell"];
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:@"cell"];
        cell.selectionStyle = UITableViewCellSelectionStyleNone;
        
    }
    if (model.type != 1) {
        cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
    }else{
        cell.accessoryType = UITableViewCellAccessoryNone;
    }
    cell.textLabel.text = model.title;
    cell.detailTextLabel.text = @"";
    if (indexPath.row == 20){
        cell.detailTextLabel.text = DataManager.shared.heartRateValueText;
    } else if (indexPath.row == 21) {
        cell.detailTextLabel.text = DataManager.shared.bloodPressureValueText;
    } else if (indexPath.row == 22) {
        cell.detailTextLabel.text = DataManager.shared.bloodOxygenValueText;
    }
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{

    FuncModel *model = self.data[indexPath.row];
    [self recordCommandWithTitle:model.title];
    if (model.type == 0) {
        Class class = NSClassFromString(model.page);
        if ([class isKindOfClass:RealTestViewController.class]){
            RealTestViewController* vc = [[RealTestViewController alloc] init];
            vc.type = indexPath.row;
            [self.navigationController pushViewController:vc animated:YES];
        }else{
            UIViewController *vc = [class new];
            [self.navigationController pushViewController:vc animated:YES];
        }

    }
    if (10 == indexPath.row) {
        YD_SyncFindDevCmd *tFindDevCmd = [[YD_SyncFindDevCmd alloc] initWithOnoff:1];
        [[CEProductK6 shareInstance] sendCmdToDevice:tFindDevCmd complete:nil];
    }
 
    if (17 == indexPath.row) {
        OtaViewController *vc = [[OtaViewController alloc] init];
        vc.customerId = DataManager.shared.devInfo.customer_id;
        [self.navigationController pushViewController:vc animated:YES];
    }
    if (18 == indexPath.row){
        [self sendWeather];
    }
    if (20 == indexPath.row){
        DataManager.shared.heartRateValueText = @"Measuring...";
        [self.tableView reloadRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationNone];
        CE_SyncHeartRateCmd *cmd = [[CE_SyncHeartRateCmd alloc] init];
        cmd.status = 1;
        [CEProductK6.shareInstance sendCmdToDevice:cmd complete:nil];

    }
    if (21 == indexPath.row) {
        DataManager.shared.bloodPressureValueText = @"Measuring...";
        [self.tableView reloadRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationNone];
        CE_SyncBloodPressureCmd *cmd = [[CE_SyncBloodPressureCmd alloc] init];
        cmd.status = 1;
        [CEProductK6.shareInstance sendCmdToDevice:cmd complete:nil];
    }
    if (22 == indexPath.row) {
        DataManager.shared.bloodOxygenValueText = @"Measuring...";
        [self.tableView reloadRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationNone];
        CE_SyncHeartO2Cmd *cmd = [[CE_SyncHeartO2Cmd alloc] init];
        cmd.status = 1;
        [CEProductK6.shareInstance sendCmdToDevice:cmd complete:nil];
        
        
    }
    
    if (23 == indexPath.row) {
        DataManager.shared.realHrvValueText = @"Measuring...";
        [self.tableView reloadRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationNone];
        CE_SyncRRIHRVCmd *cmd = [[CE_SyncRRIHRVCmd alloc] init];
        cmd.status = 1;
        [CEProductK6.shareInstance sendCmdToDevice:cmd complete:nil];
        
        
    }
    


}

- (void)swtAction:(UISwitch *)swt{
    
    NSInteger tag = swt.tag;
    FuncModel *model = self.data[tag];
    [self recordCommandWithTitle:[NSString stringWithFormat:@"%@ %@", model.title, swt.on ? @"On" : @"Off"]];
    if (3 == tag){
        YD_SyncSMSAlarmCmd *tSMSAlarmCmd = [[YD_SyncSMSAlarmCmd alloc] init];
        tSMSAlarmCmd.onoff = swt.on;
        [[CEProductK6 shareInstance] sendCmdToDevice:tSMSAlarmCmd complete:nil];
    }else if (4 == tag){
        YD_SyncCallAlarmCmd *tSMSAlarmCmd = [[YD_SyncCallAlarmCmd alloc] initWithOnoff:swt.on];
        [[CEProductK6 shareInstance] sendCmdToDevice:tSMSAlarmCmd complete:nil];
    }else if (5 == tag){
        CE_SyncLongSitRemindCmd *longSitCmd = [[CE_SyncLongSitRemindCmd alloc] init];
        longSitCmd.onoff = swt.on;
        longSitCmd.repeatDay = 0;
        longSitCmd.startTimeHour = 9;
        longSitCmd.startTimemin = 0;
        longSitCmd.endTimeHour = 20;
        longSitCmd.endTimeMin = 0;
        longSitCmd.noon_onoff = swt.on;
        [[CEProductK6 shareInstance] sendCmdToDevice:longSitCmd complete:nil];
    }else if (6 == tag) {
        YD_SyncFinishGoalCmd *tSMSAlarmCmd = [[YD_SyncFinishGoalCmd alloc] init];
        tSMSAlarmCmd.onoff = swt.on;
        [[CEProductK6 shareInstance] sendCmdToDevice:tSMSAlarmCmd complete:nil];
    }else if (7 == tag){
        YD_SyncDrinkAlarmCmd *drinkCmd = [[YD_SyncDrinkAlarmCmd alloc] init];
        drinkCmd.onoff = swt.on;
        drinkCmd.start_hour = 9;
        drinkCmd.start_min = 0;
        drinkCmd.end_hour = 20;
        drinkCmd.end_min = 59;
        drinkCmd.interval = 30;
        [[CEProductK6 shareInstance] sendCmdToDevice:drinkCmd complete:nil];
    }else if (8 == tag){
        DataManager.shared.date.format = swt.on;
        uint32_t absTime = [[NSDate date] timeIntervalSince1970];
        NSInteger offsetTime = (uint32_t)[NSTimeZone defaultTimeZone].secondsFromGMT;
        CE_SyncTimeCmd *timeCmd = [[CE_SyncTimeCmd alloc] initWithAbsTime:absTime
                                                                   offset:offsetTime
                                                                   format:DataManager.shared.date.format
                                                                 mdFormat:DataManager.shared.date.mdFormat];
        [[CEProductK6 shareInstance] sendCmdToDevice:timeCmd complete:nil];
    }else if (9 == tag){
        DataManager.shared.date.mdFormat = swt.on;
        uint32_t absTime = [[NSDate date] timeIntervalSince1970];
        NSInteger offsetTime = (uint32_t)[NSTimeZone defaultTimeZone].secondsFromGMT;
        CE_SyncTimeCmd *timeCmd = [[CE_SyncTimeCmd alloc] initWithAbsTime:absTime
                                                                   offset:offsetTime
                                                                   format:DataManager.shared.date.format
                                                                 mdFormat:DataManager.shared.date.mdFormat];
        [[CEProductK6 shareInstance] sendCmdToDevice:timeCmd complete:nil];
    }else if (11 == tag){
        CE_SyncDisturbCmd *disturbCmd = [[CE_SyncDisturbCmd alloc] init];
        NSMutableArray *disturbsArray = [NSMutableArray array];
        CE_DisturbItem *item = [[CE_DisturbItem alloc] init];
        item.start_time_hour = 9;
        item.start_time_min = 0;
        item.end_time_hour = 20;
        item.end_time_min = 59;
        [disturbsArray addObject:item];
        disturbCmd.disturbItems = disturbsArray;
        disturbCmd.switch_flag = swt.on;
        [[CEProductK6 shareInstance] sendCmdToDevice:disturbCmd complete:nil];
    }else if (12 == tag){
        YD_SyncUpBrightCmd *upBrightCmd = [[YD_SyncUpBrightCmd alloc] init];
        upBrightCmd.onoff = swt.on;
        upBrightCmd.start_hour = 9;
        upBrightCmd.start_min = 0;
        upBrightCmd.end_hour = 20;
        upBrightCmd.end_min = 59;
        [[CEProductK6 shareInstance] sendCmdToDevice:upBrightCmd complete:nil];
    }else if (13 == tag){
        YD_SyncAutoHeartCmd *autoHeartCmd = [[YD_SyncAutoHeartCmd alloc] init];
        autoHeartCmd.onoff = swt.on;
        [[CEProductK6 shareInstance] sendCmdToDevice:autoHeartCmd complete:nil];
    }else if (14 == tag){
        CE_SendPhotoCmd* photoCmd = [[CE_SendPhotoCmd alloc] init];
        photoCmd.onoff = swt.on;
        [[CEProductK6 shareInstance] sendCmdToDevice:photoCmd complete:nil];
    }else if (15 == tag){
        self.ppGswitch = !self.ppGswitch;
        CE_SyncHeartRatePPGCmd *cmd = [[CE_SyncHeartRatePPGCmd alloc] init];
        cmd.status = self.ppGswitch;
        [CEProductK6.shareInstance sendCmdToDevice:cmd complete:nil];
 
    } else if (16 == tag){
        self.gsensorswitch = !self.gsensorswitch;
        CE_SyncGsensorRawCmd *cmd = [[CE_SyncGsensorRawCmd alloc] init];
        cmd.status = self.gsensorswitch;
        [CEProductK6.shareInstance sendCmdToDevice:cmd complete:nil];
 
    }
}

- (void)sendWeather{
    CE_SyncWeatherCmd *weatherCmd = [[CE_SyncWeatherCmd alloc] init];
    weatherCmd.time = (uint32_t)[[NSDate date] timeIntervalSince1970];
    NSInteger index = 0;
    // weatherModel.forecasts 数组长度小于3数据会发送，设备侧不会处理
    for (int i=0; i<3; i++) {
        CE_WeatherItem *weatherItem = [[CE_WeatherItem alloc] init];
       
        // weatherType = 0，使用旧协议的天气类型
        weatherItem.weather = 0;
        if (index == 0) {
            weatherItem.low_temperature = 28;
            weatherItem.high_temperature = 31;
            weatherCmd.todyWeather = weatherItem;
        } else if (index == 1) {
            weatherItem.low_temperature = 27;
            weatherItem.high_temperature = 32;
            weatherCmd.tomorrowWeather = weatherItem;
        } else if (index == 2) {
            weatherItem.low_temperature = 29;
            weatherItem.high_temperature = 30;
            weatherCmd.dayAfterTomorrowWeather = weatherItem;
        }else {
            break;
        }
        index ++;
    }
    
    [[CEProductK6 shareInstance] sendCmdToDevice:weatherCmd complete:^(NSError *error) {
        if (error == nil){
            [self appendLog:@"Weather sent successfully" direction:@"DONE"];
        } else {
            [self appendLog:error.localizedDescription ?: @"Failed to send weather" direction:@"ERR"];
        }
    }];
}

- (void)updateCell:(ListSwtCell *)cell atIndexPath:(NSIndexPath *)indexPath{
    
    DataManager *shared = DataManager.shared;
    if (3 == indexPath.row){
        cell.swt.on = shared.smsSwt.onoff;
    }
    if (4 == indexPath.row){
        cell.swt.on = shared.callSwt.onoff;
    }
    if (5 == indexPath.row) {
        cell.swt.on = shared.longSit.isOpen;
    }
    if (6 == indexPath.row) {
        cell.swt.on = shared.targetSwt.onoff;
    }
    if (7 == indexPath.row){
        cell.swt.on = shared.waterSwt.isOpen;
    }
    if (8 == indexPath.row) {
        cell.swt.on = shared.date.format;
        cell.detailTextLabel.text = shared.date.format == 0 ? @"12-hour" : @"24-hour";
    }
    if (9 == indexPath.row) {
        cell.swt.on = shared.date.mdFormat;
        cell.detailTextLabel.text = shared.date.mdFormat == 0 ? @"MM-DD" : @"DD-MM";
    }
    if (11 == indexPath.row){
        cell.swt.on = shared.disturb.switch_flag;
    }
    if (12 == indexPath.row){
        cell.swt.on = shared.raiseSwt.onoff;
    }
    if (13 == indexPath.row){
        cell.swt.on = shared.autoHrSwt.onoff;
    }
    if (14 == indexPath.row){
        cell.swt.on = shared.takePhotoSwt.onoff;
    }
    
    if (15 == indexPath.row){
        cell.swt.on = self.ppGswitch;
    }
    
    if (16 == indexPath.row){
        cell.swt.on = self.gsensorswitch;
    }
    
}


@end
