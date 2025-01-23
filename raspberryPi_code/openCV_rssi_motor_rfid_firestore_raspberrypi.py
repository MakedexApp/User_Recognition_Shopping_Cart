import RPi.GPIO as GPIO

from mfrc522 import SimpleMFRC522

import firebase_admin

from firebase_admin import credentials, firestore

import time



# Firebase 초기화

cred = credentials.Certificate("/home/user/Downloads/qwer2.json")  # 서비스 계정 JSON 파일 경로

firebase_admin.initialize_app(cred)



# Firestore 클라이언트 생성

db = firestore.client()



# RFID 리더 객체 생성

reader = SimpleMFRC522()



# 태그 ID에 대응되는 과자 이름 딕셔너리 생성

snack_names = {

    "599083483996": "a",
import RPi.GPIO as GPIO

import time

from google.cloud import firestore

from google.oauth2 import service_account

import cv2

import numpy as np

import threading



# 서비스 계정 JSON 파일 경로를 지정하여 인증 정보를 설정합니다.

cred = service_account.Credentials.from_service_account_file("/home/user/Downloads/qwer2.json")



# Firestore 초기화 (자격 증명 추가)

db = firestore.Client(credentials=cred)



# GPIO 핀 번호 설정

IN1 = 17

IN2 = 27

ENA = 18

IN3 = 22

IN4 = 23

ENB = 13



# GPIO 모드 설정

GPIO.setmode(GPIO.BCM)

GPIO.setwarnings(False)



# 핀 모드 설정

GPIO.setup(IN1, GPIO.OUT)

GPIO.setup(IN2, GPIO.OUT)

GPIO.setup(ENA, GPIO.OUT)

GPIO.setup(IN3, GPIO.OUT)

GPIO.setup(IN4, GPIO.OUT)

GPIO.setup(ENB, GPIO.OUT)



# PWM 설정

pwm_a = GPIO.PWM(ENA, 3000)  # 3kHz 주파수

pwm_b = GPIO.PWM(ENB, 3000)  # 3kHz 주파수



pwm_a.start(0)  # PWM 듀티 사이클 0%

pwm_b.start(0)  # PWM 듀티 사이클 0%



# 모터 제어 함수들 (전진, 정지, 좌회전, 우회전)

def motor_forward(speed):

    GPIO.output(IN1, GPIO.HIGH)

    GPIO.output(IN2, GPIO.LOW)

    GPIO.output(IN3, GPIO.HIGH)

    GPIO.output(IN4, GPIO.LOW)

    pwm_a.ChangeDutyCycle(speed)

    pwm_b.ChangeDutyCycle(speed)



def motor_stop():

    GPIO.output(IN1, GPIO.LOW)

    GPIO.output(IN2, GPIO.LOW)

    GPIO.output(IN3, GPIO.LOW)

    GPIO.output(IN4, GPIO.LOW)

    pwm_a.ChangeDutyCycle(0)

    pwm_b.ChangeDutyCycle(0)



def motor_left(speed):

    pwm_a.ChangeDutyCycle(speed * 0.3)  # 왼쪽 바퀴 느리게

    pwm_b.ChangeDutyCycle(speed)  # 오른쪽 바퀴 빠르게



def motor_right(speed):

    pwm_a.ChangeDutyCycle(speed)  # 왼쪽 바퀴 빠르게

    pwm_b.ChangeDutyCycle(speed * 0.3)  # 오른쪽 바퀴 느리게



# HSV 필터로 노란색 옷 검출

def detect_yellow_object(frame):

    hsv_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

    lower_yellow = np.array([20, 100, 100])

    upper_yellow = np.array([30, 255, 255])

    mask = cv2.inRange(hsv_frame, lower_yellow, upper_yellow)

    return mask



# 카메라 화면에서 사용자의 위치를 기반으로 모터 제어

def control_cart_based_on_position(mask, frame_width, speed):

    contours, _ = cv2.findContours(mask, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

    if len(contours) > 0:

        largest_contour = max(contours, key=cv2.contourArea)

        x, y, w, h = cv2.boundingRect(largest_contour)

        center_x = x + w // 2  # 사용자의 중심 x좌표



        if center_x > frame_width * 2 / 3:

            print("사용자가 오른쪽에 있습니다. 카트 우회전.")

            motor_right(speed)

        elif center_x < frame_width / 3:

            print("사용자가 왼쪽에 있습니다. 카트 좌회전.")

            motor_left(speed)

        else:

            print("사용자가 중앙에 있습니다. 카트 직진.")

            motor_forward(speed)

    else:

        print("노란색 객체가 감지되지 않았습니다.")

        motor_stop()



# 전역 변수로 RSSI 값을 유지

current_rssi = -100  # 기본값을 낮은 값으로 설정



# Firestore에서 실시간으로 RSSI 값을 업데이트하는 스레드 함수

def listen_for_rssi():

    global current_rssi

    doc_ref = db.collection("rssi").document("check_rssi")



    # Firestore 실시간 업데이트 리스너

    def on_snapshot(doc_snapshot, changes, read_time):

        global current_rssi

        for doc in doc_snapshot:

            current_rssi = doc.to_dict().get("data")

            print(f"현재 RSSI 값: {current_rssi}")



    # 문서 스냅샷 리스너 설정

    doc_ref.on_snapshot(on_snapshot)



# 카메라와 모터를 제어하는 메인 루프

def control_motor_with_camera():

    camera = cv2.VideoCapture(0)
    

    if not camera.isOpened():

        print("카메라를 열 수 없습니다.")

        return



    frame_width = int(camera.get(cv2.CAP_PROP_FRAME_WIDTH))



    while True:

        ret, frame = camera.read()

        if not ret:

            print("카메라에서 영상을 읽어올 수 없습니다.")

            break



        # 노란색 객체 검출

        mask = detect_yellow_object(frame)

        print(f"current rssi value : {current_rssi}")

        # RSSI 값이 기준 이하일 때만 모터 제어

        if current_rssi <= -60:
            print("RSSI is under the standard. motor on...")
            control_cart_based_on_position(mask, frame_width, 100)

        else:

            print("RSSI 값이 기준 이상입니다. 모터 정지.")

            motor_stop()



        # 결과 보여주기 (원하는 경우 주석 해제)

        cv2.imshow("Frame", frame)

        cv2.imshow("Mask", mask)



        if cv2.waitKey(1) & 0xFF == ord('q'):

            break



    camera.release()

    cv2.destroyAllWindows()



# 프로그램 시작

try:

    print("Firestore에서 실시간 RSSI 값을 가져옵니다.")

    

    # RSSI 업데이트 스레드 시작

    rssi_thread = threading.Thread(target=listen_for_rssi)

    rssi_thread.daemon = True

    rssi_thread.start()



    # 메인 모터 제어 루프 실행

    control_motor_with_camera()



except KeyboardInterrupt:

    print("프로그램이 종료되었습니다.")



finally:

    pwm_a.stop()

    pwm_b.stop()

    GPIO.cleanup()

    camera.release()

    cv2.destroyAllWindows()


    "874229826540": "b",

    "598848734028": "c",

    "882062786969": "d",

    "906249822851": "p",

    "280210350347": "e"

}



# 마지막으로 읽은 태그 ID 저장 변수

last_id = None

tag_in_range = False  # 태그가 리더 범위 내에 있는지 여부를 추적하는 플래그



def send_to_firestore(tag_id):

    """Firestore에 태그 ID를 사용하여 카운트를 저장"""

    doc_ref = db.collection('products').document(str(tag_id))



    try:

        # Firestore에서 현재 카운트를 가져오기

        doc = doc_ref.get()

        if doc.exists:

            # 문서가 이미 존재하면 카운트를 증가

            current_count = doc.to_dict().get('count', 0)

            new_count = current_count + 1

            doc_ref.update({'count': new_count})  # 카운트 업데이트

            print(f"기존 문서 업데이트: {new_count}")

        else:

            # 문서가 존재하지 않으면 새로 생성하고 카운트를 1로 설정

            doc_ref.set({'count': 1})

            print(f"새 문서 생성: {tag_id} 카운트 1로 설정")

    except Exception as e:

        print(f"Firestore 업데이트 오류: {e}")



try:

    while True:

        print("RFID 태그를 리더에 가져다 대세요...")

        try:

            # RFID 태그의 ID 읽기

            id, text = reader.read_no_block()  # 태그가 없는 경우 None 반환

        except Exception:

            id = None  # 태그가 없으면 None으로 처리



        if id is not None:

            # 태그가 감지된 경우

            if not tag_in_range:

                # 태그가 처음 감지된 경우 (뗐다가 다시 태그한 상황)

                snack_name = snack_names.get(str(id), "Unknown snack")

                print(f"태그 ID: {id}, 과자 이름: {snack_name}")

                send_to_firestore(id)

                tag_in_range = True  # 태그가 감지된 상태로 플래그 설정

                last_id = id  # 마지막으로 읽은 태그 ID 업데이트



        else:

            # 태그가 감지되지 않으면 태그 범위에서 벗어난 것으로 처리

            tag_in_range = False  # 태그가 범위를 벗어나면 플래그를 False로



        # 태그를 떼는 시간 동안 대기 (0.5초)

        time.sleep(0.5)



except KeyboardInterrupt:

    # 프로그램 종료 시 GPIO 클린업

    GPIO.cleanup()

    print("프로그램 종료")