# A fluid draggable menu

Inspired by the [original work](https://twitter.com/jmtrivedi/status/1610017363218563072?s=20&t=PP2YsTMOL5FYV4TWfka7cw) from Janum Trivedi ([jmtrivedi@twitter](https://twitter.com/jmtrivedi)), the draggable bouncy menu in Jetpack Compose is here.

<p align="center"><b>Sample GIFs</b></p>

| ![](images/sample_press_anchor_to_drag.gif) | ![](images/sample_press_item_to_drag.gif) | ![](images/sample_dark.gif) |
|:-------------------------------------------:|:-----------------------------------------:|:---------------------------:|
|            Press anchor to drag             |          Press menu item to drag          |            Dark             |

The simplified sample code would be like this:

```kotlin
@Composable
fun MenuSample(modifier: Modifier = Modifier) {
    val state = rememberDraggableMenuState()
	
    Box(
        modifier = modifier
            .fillMaxSize()
            .draggableMenuContainer(state),
    ) {
        Box(modifier = Modifier.align(Alignment.Center)) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.draggableMenuAnchor(state),
            )

            DraggableMenu(state = state, onItemSelected = {}) {
                items(5) {
                    Text("Menu item: $it")
                }
            }
        }
    }
}
```

# License

```
Copyright 2023 dokar3

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
