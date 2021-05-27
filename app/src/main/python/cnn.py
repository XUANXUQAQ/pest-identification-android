import json
import os
import torch
from PIL import Image
from torch import nn
from torch.autograd import Variable
from torchvision.transforms import transforms as tt
import pycurl

BATCH_SIZE = 32
LR = 0.01
IMAGE_SIZE = 608
EPOCH = 10

classes = ['C15204005005', 'C15104005005', 'C15331005010', 'C15206040005', 'C22301125010',
           'C22301070005',
           'C22338005020', 'C22345105005', 'C22345170045', 'C22359040060', 'C22342015010',
           'C22342015005',
           'C22342135005', 'C21701825005', 'C21701065030', 'C21701080005', 'C21701445005',
           'C21701095005',
           'C21701065010', 'C21701130015', 'C21109005005', 'C15204035010', 'C15204020005',
           'C15408115005',
           'C15408150000', 'C15408140005', 'C15408105005', 'C15409015010', 'C15410010005',
           'C15428005005',
           'C15105015045', 'C15105005005', 'C15102015005', 'C15418003005', 'C15106005010',
           'C15206070025',
           'C15401135015', 'C15401030015', 'C15403040010', 'C15401135010', 'C22355005010',
           'C22355005005',
           'C22302060030', 'C22302035005', 'C22331010005', 'C22331375010', 'C22331145005',
           'C22301090015',
           'C22338005025', 'C22345075010', 'C22345005005', 'C22345055005', 'C22345170050',
           'C22345120015',
           'C22348075005', 'C22348090020', 'C22348100020', 'C22348030040', 'C22358060010',
           'C22359040005',
           'C22359040025', 'C22359040055', 'C22359010005', 'C22359050005', 'C22359040045',
           'C22359020015',
           'C22330030010', 'C22320010005', 'C22360210010', 'C22360190005', 'C22360095010',
           'C22360185010',
           'C22360010005', 'C22336025040', 'C22326080040', 'C22326060005', 'C22327100015',
           'C22327065005',
           'C22341055010', 'C22341090005', 'C22341150010', 'C22341185030', 'C22341025005',
           'C22341200015',
           'C22341165010', 'C22346420005', 'C22346290005', 'C22346715005', 'C22346870005',
           'C22346725010',
           'C22342285005', 'C22342010005', 'C21102055005', 'C21102020020', 'C21301095005',
           'C21108045005',
           'C21701690005', 'C21701080010', 'C21703280010']
net: nn.Module

transforms = tt.Compose([tt.Resize((IMAGE_SIZE, IMAGE_SIZE)), tt.ToTensor()])


class CNN(nn.Module):

    def __init__(self):
        super(CNN, self).__init__()
        self.conv1 = nn.Sequential(
            nn.Conv2d(in_channels=3, out_channels=6, kernel_size=(
                3, 3), stride=(1, 1), padding=(2, 2)),
            nn.ReLU(), nn.MaxPool2d(2, 2))

        self.conv2 = nn.Sequential(nn.Conv2d(in_channels=6, out_channels=16, kernel_size=(5, 5)),
                                   nn.ReLU(),
                                   nn.MaxPool2d(2, 2))

        self.fc1 = nn.Sequential(nn.Linear(in_features=16 * 150 * 150, out_features=300),
                                 nn.BatchNorm1d(300), nn.ReLU())

        self.fc2 = nn.Sequential(
            nn.Linear(300, 150),
            nn.BatchNorm1d(150),
            nn.ReLU(),
            nn.Linear(150, len(classes)))

    def forward(self, x):
        x = self.conv1(x)
        x = self.conv2(x)
        x = x.view(x.size()[0], -1)
        x = self.fc1(x)
        x = self.fc2(x)
        return x


def init(classes_url):
    global net
    __set_classes(classes_url)
    net = CNN()
    net.eval()
    path = os.path.join(os.path.dirname(__file__), 'model.pt')
    pretrained_dict = torch.load(path, map_location=torch.device('cpu'))
    net.load_state_dict(pretrained_dict)


def __set_classes(classes_url):
    global classes
    c = pycurl.Curl()
    c.setopt(pycurl.URL, classes_url)
    c.setopt(pycurl.TIMEOUT, 5)
    c.setopt(pycurl.CONNECTTIMEOUT, 5)
    __classes = []
    try:
        resp = c.perform_rs()
        data = json.loads(resp)
        if data['code'] == 20000:
            __classes = data['result']['classes']
            classes = __classes
        else:
            raise Exception("error")
    except Exception as e:
        print(e)
    c.close()


def predict(__images):
    img = Image.open(__images)
    img = transforms(img)
    img = Variable(img).reshape(1, 3, IMAGE_SIZE, IMAGE_SIZE)
    output_test = net(img)
    _, predicted = torch.max(output_test, 1)
    return classes[predicted.cpu().numpy()[0]]
