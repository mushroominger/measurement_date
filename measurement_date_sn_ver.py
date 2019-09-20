import os
import sys

# 時間関係
import time
import datetime
import calendar
import sched

# urlをデフォルトのウェブブラウザで開く
import webbrowser

# gui周り
import tkinter as tk
from tkinter import messagebox as tkmsg

# 音声再生Windows用
# import winsound

# 音声再生用
# import vlc

# 設定時刻タプル
set_times = (
    (13, 20),
    (15, 20),
    (16, 0)
)


###### syukujitsu.csv を読み込んでlist化する ######

def holidays():
    # 祝日.csvのありか
    url = 'https://www8.cao.go.jp/chosei/shukujitsu/gaiyou.html#syukujitu'
    # 同階層の祝日.csvのファイルパスを通す
    file_name = 'syukujitsu.csv'
    dir_path = os.path.dirname(sys.argv[0])
    file_path = '%s/%s' % (dir_path, file_name)

    # 祝日.csvを読み込む
    with open(file_path, 'r', encoding='shift_jis') as csv_file:
        loaded_csv = csv_file.read()

    # 改行でlist化
    holidays_array = loaded_csv.split('\n')

    # タイトル部分と末尾の空白要素を削除
    del holidays_array[0]
    if holidays_array[-1] == '':
        del holidays_array[-1]

    # なんの休みの日かは要らないので削除
    for i in range(len(holidays_array)):
        holidays_array[i] = holidays_array[i].split(',')
        del holidays_array[i][1]
        holidays_array[i] = holidays_array[i][0]

    if holidays_array[-1][:4] == datetime.date.today().year:
        root = tk.Tk()
        root.withdraw()
        # winsound.PlaySound('SystemExit', winsound.SND_ALIAS)
        tkmsg.askyesno('インフォメーション', '祝日.csvの更新をしてください')
        webbrowser.open(url)

    return holidays_array


###### 検針予定日が休日ならFalseを返す関数 ######

def holiday_checker(measurement_date):
    switch_bool = True
    year = measurement_date.year
    month = measurement_date.month
    date = measurement_date.day
    week = measurement_date.weekday()
    print(week)
    date_str = '%s/%s/%s' % (year, month, date)
    print(date_str)

    # 休日リストに該当すればFalse
    if (date_str in holidays()):
        switch_bool = False

    # 土曜(5)と日曜(6)に該当すればFalse
    if week == 5 or week == 6:
        switch_bool = False

    print(switch_bool)
    return switch_bool


###### 検針日が土日祝祭日か確認し前日に設定する ######

def measurement_date_checker():
    # 今日を取得
    today = datetime.date.today()
    # 月末日を取得
    _, lastday = calendar.monthrange(today.year, today.month)
    # 基本になる月末の検針日を取得
    measurement_date = datetime.date(today.year, today.month, lastday)

    # test通知日を今日にする
    # measurement_date = datetime.date(today.year, today.month, today.day)

    # 十二月なら検針日を十二月二十八日に変更する
    if measurement_date.month == 12:
        measurement_date = datetime.date(today.year, 12, 28)

    # 所定日が休日(False)なら前日に前倒しする
    while holiday_checker(measurement_date) == False:
        measurement_date = measurement_date - datetime.timedelta(days=1)

    return measurement_date


###### 日時をエポック秒に変換 ######

def datetime2epoch(set_time):
    now = datetime.date.today()
    dt = datetime.datetime(now.year, now.month, now.day, set_time[0], set_time[1])
    dt = time.mktime(dt.timetuple())
    return dt


###### 終了か継続か ######

def kill_or_nothing():
    global b_bool
    if b_bool:
        snooze()
    else:
        all_kill()


###### 通知ボタンの削除・終了 ######

def all_kill():
    root.destroy()
    sys.exit()


###### 検針日以外だった場合 ######

def other_day(msg):
    global root, l_strings, b_strings, b_bool
    l_strings.set(msg)
    b_strings.set('OK')
    b_bool = False
    root.deiconify()


###### 検針日当日だった場合 ######

def snooze():
    global root
    root.withdraw()

    # 13時20分の通知時間をエポック秒で取得
    first_time = datetime2epoch(set_times[0])

    # 15時20分の通知時間をエポック秒で取得
    second_time = datetime2epoch(set_times[1])

    # 16時の通知時間をエポック秒で取得
    last_time = datetime2epoch(set_times[2])

    now = time.time()

    if last_time >= now:
        if first_time >= now:
            sched_time = first_time
            strings = 'スヌーズ'
            continue_bool = True
            sound_bool = False

        elif second_time >= now:
            sched_time = second_time
            strings = 'スヌーズ'
            continue_bool = True
            sound_bool = True

        else:
            sched_time = last_time
            strings = 'OK'
            continue_bool = False
            sound_bool = True

        schedule = sched.scheduler(time.time, time.sleep)
        schedule.enterabs(sched_time, 1, on_the_day, (strings, continue_bool, sound_bool))
        schedule.run()

    else:
        all_kill()

def on_the_day(strings, continue_bool, sound_bool):
    global root, l_strings, b_strings, b_bool
    # 音声のファイルパスを通す
    wav_name = 'XXXX.wav'
    dir_path = os.path.dirname(sys.argv[0])
    wav_path = '%s/%s' % (dir_path, wav_name)
    if sound_bool:
        print('音声再生')
        with open(wav_path, 'rb') as wav_file:
            wav_data = wav_file.read()
        # winsound.PlaySound(wav_file, winsound.SND_MEMORY)
        # p = vlc.MediaPlayer()
        # p.set_mrl(wav_name)
        # p.play()
    l_strings.set('検針日です')
    b_strings.set(strings)
    b_bool = continue_bool
    root.deiconify()


###### main ######

def main():
    global root, l_strings, b_strings, b_bool
    # 今日を取得
    today = datetime.date.today()
    measurement_date = measurement_date_checker()

    root = tk.Tk()
    root.attributes('-topmost', True)
    root.title('検針日のお知らせ')
    l_strings = tk.StringVar()
    b_strings = tk.StringVar()
    b_bool = True
    print(b_bool)
    label = tk.Label(root, textvariable=l_strings)
    label.pack(fill='x')
    button = tk.Button(root, textvariable=b_strings, command=kill_or_nothing)
    button.pack(fill='x')
    root.withdraw()

    last_time = datetime2epoch(set_times[2])

    if measurement_date == today:
        if last_time >= time.time():
            on_the_day('スヌーズ', True, True)
        else:
            on_the_day('OK', False, True)

    elif (measurement_date - today) == 1:
        other_day('検針日は明日です')

    else:
        other_day('検針日は%sです' % measurement_date.strftime('%Y/%m/%d'))

    root.mainloop()


if __name__ == "__main__":
    main()