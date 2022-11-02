package org.rfcx.companion.view.deployment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.fragment_image_labeling.*
import org.rfcx.companion.R
import org.rfcx.companion.view.deployment.guardian.GuardianDeploymentProtocol
import org.rfcx.companion.view.deployment.songmeter.SongMeterDeploymentProtocol

class ImageLabelingFragment : Fragment() {

    private var deploymentProtocol: BaseDeploymentProtocol? = null
    private var imagePaths: Array<String> = arrayOf()

    private var currentImage = -1
    private var checkedLabel: Chip? = null
    private var mapImageLabel: MutableMap<String, String> = mutableMapOf()
    private var labelId = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        when (context) {
            is AudioMothDeploymentProtocol -> labelId = R.array.audiomoth_labels
            is SongMeterDeploymentProtocol -> labelId = R.array.audiomoth_labels
            is GuardianDeploymentProtocol -> labelId = R.array.audiomoth_labels
        }
        deploymentProtocol = context as BaseDeploymentProtocol
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_labeling, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imagePaths = it.getStringArray(IMAGE_PATHS) as Array<String>
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addLabels()
        nextImage()
        nextButton.setOnClickListener {
            mapImageLabel[imagePaths[currentImage]] = checkedLabel?.text.toString()
            if (currentImage == (imagePaths.size - 1)) {
                deploymentProtocol?.setImageLabels(mapImageLabel)
                deploymentProtocol?.backStep()
            } else {
                nextImage()
            }
        }
    }

    private fun addLabels() {
        val labels = requireContext().resources.getStringArray(labelId)
        labels.forEach {
            addChip(it)
        }

        labelChipGroup.setOnCheckedChangeListener { group, checkedId ->
            checkedLabel = group.findViewById(checkedId)
            nextButton.isEnabled = true
        }
    }

    private fun addChip(name: String) {
        val chip = layoutInflater.inflate(R.layout.label_chip, labelChipGroup, false) as Chip
        chip.text = name
        chip.id = ViewCompat.generateViewId()
        labelChipGroup.addView(chip)
    }

    private fun nextImage() {
        currentImage++
        Glide.with(requireContext())
            .load(imagePaths[currentImage])
            .placeholder(R.drawable.bg_placeholder_light)
            .error(R.drawable.bg_placeholder_light)
            .into(image)
    }

    companion object {
        private const val IMAGE_PATHS = "IMAGE_PATHS"

        fun newInstance(imagePaths: Array<String>): ImageLabelingFragment {
            return ImageLabelingFragment().apply {
                arguments = Bundle().apply {
                    putStringArray(IMAGE_PATHS, imagePaths)
                }
            }
        }
    }
}
